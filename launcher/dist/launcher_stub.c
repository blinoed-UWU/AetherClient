#define UNICODE
#define _UNICODE
#include <windows.h>
#include <shellapi.h>
#include <shlobj.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int FileExists(const wchar_t *path) {
    DWORD attr = GetFileAttributesW(path);
    return (attr != INVALID_FILE_ATTRIBUTES && !(attr & FILE_ATTRIBUTE_DIRECTORY));
}

static int DirectoryExists(const wchar_t *path) {
    DWORD attr = GetFileAttributesW(path);
    return (attr != INVALID_FILE_ATTRIBUTES && (attr & FILE_ATTRIBUTE_DIRECTORY));
}

static void GetExeDir(wchar_t *out, DWORD maxLen) {
    GetModuleFileNameW(NULL, out, maxLen);
    wchar_t *lastSlash = wcsrchr(out, L'\\');
    if (lastSlash) {
        *lastSlash = L'\0';
    }
}

static int FindJavaExecutable(wchar_t *outJava, DWORD maxLen) {
    wchar_t exeDir[MAX_PATH];
    GetExeDir(exeDir, MAX_PATH);

    // 1. Check relative ./runtime/bin/javaw.exe
    _snwprintf(outJava, maxLen, L"%s\\runtime\\bin\\javaw.exe", exeDir);
    if (FileExists(outJava)) return 1;

    // 2. Check JAVA_HOME environment variable
    wchar_t envJava[MAX_PATH];
    if (GetEnvironmentVariableW(L"JAVA_HOME", envJava, MAX_PATH) > 0) {
        _snwprintf(outJava, maxLen, L"%s\\bin\\javaw.exe", envJava);
        if (FileExists(outJava)) return 1;
    }

    // 3. Check JDK_HOME
    if (GetEnvironmentVariableW(L"JDK_HOME", envJava, MAX_PATH) > 0) {
        _snwprintf(outJava, maxLen, L"%s\\bin\\javaw.exe", envJava);
        if (FileExists(outJava)) return 1;
    }

    // 4. Search Program Files common JDK paths (Temurin, Oracle, Microsoft, Zulu, BellSoft)
    const wchar_t *searchRoots[] = {
        L"C:\\Program Files\\Eclipse Adoptium",
        L"C:\\Program Files\\Java",
        L"C:\\Program Files\\Microsoft",
        L"C:\\Program Files\\Zulu",
        L"C:\\Program Files\\BellSoft",
        NULL
    };

    for (int i = 0; searchRoots[i] != NULL; i++) {
        if (!DirectoryExists(searchRoots[i])) continue;
        wchar_t findPattern[MAX_PATH];
        _snwprintf(findPattern, MAX_PATH, L"%s\\*", searchRoots[i]);
        WIN32_FIND_DATAW fd;
        HANDLE hFind = FindFirstFileW(findPattern, &fd);
        if (hFind != INVALID_HANDLE_VALUE) {
            do {
                if ((fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) && wcscmp(fd.cFileName, L".") != 0 && wcscmp(fd.cFileName, L"..") != 0) {
                    _snwprintf(outJava, maxLen, L"%s\\%s\\bin\\javaw.exe", searchRoots[i], fd.cFileName);
                    if (FileExists(outJava)) {
                        FindClose(hFind);
                        return 1;
                    }
                }
            } while (FindNextFileW(hFind, &fd));
            FindClose(hFind);
        }
    }

    // 5. Check PATH for javaw.exe
    wchar_t pathBuf[MAX_PATH];
    if (SearchPathW(NULL, L"javaw.exe", NULL, MAX_PATH, pathBuf, NULL) > 0) {
        wcsncpy(outJava, pathBuf, maxLen);
        return 1;
    }

    return 0;
}

static int ExtractResourceToFile(HINSTANCE hInstance, int resId, const wchar_t *resType, const wchar_t *targetPath) {
    HRSRC hRes = FindResourceW(hInstance, MAKEINTRESOURCEW(resId), resType);
    if (!hRes) return 0;
    HGLOBAL hData = LoadResource(hInstance, hRes);
    DWORD dataSize = SizeofResource(hInstance, hRes);
    if (!hData || dataSize == 0) return 0;
    void *pData = LockResource(hData);
    if (!pData) return 0;

    // Check if target file already exists and has matching size
    HANDLE hCheck = CreateFileW(targetPath, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hCheck != INVALID_HANDLE_VALUE) {
        LARGE_INTEGER size;
        if (GetFileSizeEx(hCheck, &size) && (DWORD)size.QuadPart == dataSize) {
            CloseHandle(hCheck);
            return 1;
        }
        CloseHandle(hCheck);
    }

    HANDLE hOut = CreateFileW(targetPath, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hOut == INVALID_HANDLE_VALUE) return 0;
    DWORD written = 0;
    WriteFile(hOut, pData, dataSize, &written, NULL);
    CloseHandle(hOut);
    return (written == dataSize);
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PWSTR pCmdLine, int nCmdShow) {
    // Enable High-DPI awareness
    SetProcessDPIAware();

    wchar_t exeDir[MAX_PATH];
    GetExeDir(exeDir, MAX_PATH);

    wchar_t javaExe[MAX_PATH];
    if (!FindJavaExecutable(javaExe, MAX_PATH)) {
        int choice = MessageBoxW(
            NULL,
            L"Для запуска AetherLauncher требуется установленная Java 21 (64-bit).\n\n"
            L"Java не найдена на вашем компьютере.\n\n"
            L"Открыть страницу загрузки Eclipse Temurin Java 21?",
            L"AetherLauncher - Java 21 не найдена",
            MB_ICONQUESTION | MB_YESNO | MB_DEFBUTTON1
        );
        if (choice == IDYES) {
            ShellExecuteW(NULL, L"open", L"https://adoptium.net/temurin/releases/?version=21", NULL, NULL, SW_SHOWNORMAL);
        }
        return 1;
    }

    // Determine path to AetherLauncher.jar
    wchar_t jarPath[MAX_PATH];
    _snwprintf(jarPath, MAX_PATH, L"%s\\AetherLauncher.jar", exeDir);

    // If companion jar does not exist next to exe, extract from embedded resource if present
    if (!FileExists(jarPath)) {
        wchar_t localAppData[MAX_PATH];
        if (SUCCEEDED(SHGetFolderPathW(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, localAppData))) {
            wchar_t cacheDir[MAX_PATH];
            _snwprintf(cacheDir, MAX_PATH, L"%s\\AetherClient", localAppData);
            CreateDirectoryW(cacheDir, NULL);
            _snwprintf(jarPath, MAX_PATH, L"%s\\AetherClient\\AetherLauncher.jar", localAppData);
            ExtractResourceToFile(hInstance, 101, L"EMBEDDED_JAR", jarPath);
        }
    }

    // Also extract embedded aether-agent.jar to %APPDATA%\AetherClient\payloads\aether-agent.jar
    wchar_t roamingAppData[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathW(NULL, CSIDL_APPDATA, NULL, 0, roamingAppData))) {
        wchar_t rootDir[MAX_PATH], payloadsDir[MAX_PATH], agentPath[MAX_PATH];
        _snwprintf(rootDir, MAX_PATH, L"%s\\AetherClient", roamingAppData);
        CreateDirectoryW(rootDir, NULL);
        _snwprintf(payloadsDir, MAX_PATH, L"%s\\payloads", rootDir);
        CreateDirectoryW(payloadsDir, NULL);
        _snwprintf(agentPath, MAX_PATH, L"%s\\aether-agent.jar", payloadsDir);
        ExtractResourceToFile(hInstance, 102, L"EMBEDDED_AGENT", agentPath);
    }

    if (!FileExists(jarPath)) {
        wchar_t msg[MAX_PATH + 128];
        _snwprintf(msg, sizeof(msg) / sizeof(wchar_t),
            L"Файл лаунчера не найден:\n%s\n\nУбедитесь, что AetherLauncher.jar находится рядом с exe файлом.",
            jarPath);
        MessageBoxW(NULL, msg, L"AetherLauncher - Ошибка запуска", MB_ICONERROR | MB_OK);
        return 2;
    }

    // Build command line
    wchar_t cmdLine[4096];
    _snwprintf(cmdLine, sizeof(cmdLine) / sizeof(wchar_t),
        L"\"%s\" -Dfile.encoding=UTF-8 -XX:+UseG1GC -jar \"%s\" %s",
        javaExe, jarPath, pCmdLine ? pCmdLine : L"");

    STARTUPINFOW si;
    PROCESS_INFORMATION pi;
    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    BOOL success = CreateProcessW(
        NULL,
        cmdLine,
        NULL,
        NULL,
        FALSE,
        0,
        NULL,
        exeDir,
        &si,
        &pi
    );

    if (!success) {
        DWORD err = GetLastError();
        wchar_t errMsg[512];
        _snwprintf(errMsg, 512, L"Не удалось запустить процесс Java (Код ошибки: %lu).\nКоманда: %s", err, cmdLine);
        MessageBoxW(NULL, errMsg, L"AetherLauncher - Ошибка", MB_ICONERROR | MB_OK);
        return 3;
    }

    CloseHandle(pi.hThread);
    CloseHandle(pi.hProcess);
    return 0;
}
