# AETHERCLIENT MASTER AI AUDIT & OPTIMIZATION DIRECTIVE
> **Role Assignment**: Principal Systems Engineer & Senior Minecraft Fabric 1.21.11 / JVM Bytecode / OpenGL Architect.  
> **Target Environment**: AetherClient Backup / Staging Workspace (`backup-staging` branch).  
> **Operational Goal**: Perform an exhaustive, zero-laziness code audit, bug fixing, performance optimization, and feature expansion pass. Generate a structured, machine-parsable handoff report for the Primary Agent to review and merge into `main`.

---

## 1. MISSION CHARTER & ARCHITECTURAL INVARIANTS

You are auditing a dual-component ecosystem:
1. **AetherLauncher**: A cross-platform Kotlin Multiplatform & Compose Desktop application with dynamic HotSpot JVM agent injection, instance detection (Prism/Freesm/Modrinth/Vanilla), OTA updates via GitHub Releases, config resetting, and stealth trace cleaning/self-destruction.
2. **AetherClient & AetherAgent**: A stealth Fabric 1.21.11 client injected dynamically or pre-main via Java agent, utilizing ASM bytecode transformation (`AetherClassTransformer`), isolated `KnotClassLoader` bridging, Blaze3D OpenGL state guarding (`GlStateGuard`), hardware-accelerated Liquid Glass UI (`NanoVgEngine` + `KawaseBlur`), and low-level packet/inventory combat logic.

### NON-NEGOTIABLE INVARIANTS (FAIL-CLOSED)
1. **Zero Blaze3D Corruption**: Every call to NanoVG or custom OpenGL shaders MUST be wrapped in `GlStateGuard.save()` and `GlStateGuard.restore()`. Never leave texture units 0..3 un-restored or GL33 samplers bound to dirty IDs.
2. **Yarn Mapping Precision (1.21.11)**:
   - Window buffer swap: `net.minecraft.client.util.Window.swapBuffers(TracyFrameCapturer)` -> intermediary `method_15998` / obfuscated `a`.
   - Keyboard onKey: `net.minecraft.client.Keyboard.onKey(long, int, int, int, int)` -> intermediary `method_1457` / obfuscated `a`.
   - Sky rendering: `WorldRenderer.renderSky(MatrixStack, Matrix4f, float, Camera, boolean, Runnable)` -> intermediary `method_62302` / `method_62306`.
3. **ClassLoader Segregation**: Native LWJGL libraries (`liblwjgl_nanovg.so` / `lwjgl_nanovg.dll`) cannot be loaded twice in different classloaders. Always reuse the game's Knot/LWJGL classloader or pass calls through `AetherBridge`.
4. **Anti-Detection & Clean Unload**:
   - `aether.client.active` and `aether.agent.loaded` must be cleared from `System.getProperties()` upon unload.
   - All OpenAL audio sources, background worker threads, and event listeners must be safely stopped and disposed.
5. **No Lazy Placeholders**: Never write `// TODO`, `// Implement here`, or stub methods. All ported and new features must be 100% complete, fully typed, and verified to compile.

---

## 2. SYSTEMATIC 6-PHASE AUDIT PROTOCOL

### PHASE 1: RECONNAISSANCE & CODE GRAPH MAPPING
Before touching any code, thoroughly read and map:
- `bridge/src/main/java/dev/aetherclient/bridge/`
- `agent/src/main/java/dev/aetherclient/agent/`
- `client/src/main/java/dev/aetherclient/client/`
  - `render/` (`GlStateGuard.java`, `KawaseBlur.java`, `NanoVgEngine.java`, `VisualRenderer.java`, `Render2D.java`)
  - `gui/` (`ClickGuiController.java`, `AetherMenuScreen.java`, `hud/`)
  - `module/` (`ModuleManager.java`, `Module.java`, `impl/*`)
  - `config/` (`ConfigManager.java`)
  - `audio/` (`SoundEngine.java`)
- `launcher/src/`
  - `core/` (`JvmInjector.kt`, `ProcessDetector*.kt`, `InstanceManager.kt`, `UpdateManager.kt`, `LauncherController.kt`)
  - `ui/` (`screens/*`, `components/*`)

### PHASE 2: DEFECT TRIAGE & HIGH-SEVERITY BUG FIXING
Inspect and eliminate known defect classes:
1. **JVM HotSpot Attach Race Conditions**:
   - HotSpot's Attach Listener permits only one simultaneous client connection per target JVM.
   - Enforce a `ConcurrentHashMap<Long, Mutex>` per target PID in `JvmInjector.kt` so detection and injection never collide.
   - Check if `aether.client.active` is true BEFORE calling `attach()` or `loadAgent()`.
2. **Process Detection on Windows**:
   - `ProcessHandle.info().commandLine()` can be empty without administrative rights on Windows.
   - Add a fallback in `ProcessDetectorWindows.kt` utilizing `VirtualMachine.list()` display names and `wmic process where ProcessId=<PID> get CommandLine`.
3. **OpenGL Memory & State Leaks**:
   - In `NanoVgEngine.java`: ensure `fontRegularBuffer` (native ByteBuffer) is properly allocated off-heap and freed if NanoVG re-initializes.
   - In `KawaseBlur.java`: verify FBO ping-pong textures are resized only on viewport dimension changes and deleted on `destroy()`.
4. **Combat & Inventory Bypasses**:
   - `TripleSwapModule.java` & `FastSwapModule.java`: ensure artificial inventory opening (`OpenScreenS2CPacket` simulation or player screen state) does not desync player movement packets (`PlayerMoveC2SPacket`) and prevents movement while the swap UI is held.
   - `MaceHelperModule.java`: fall distance calculation must account for slow falling, water, and cobweb dampening.

### PHASE 3: PERFORMANCE PROFILING & ALLOCATION ELIMINATION
- **Render Loop Allocations**:
  - Audit `VisualRenderer.java` and `ClickGuiController.java`. Eliminate any `new Vector3f()`, `new MatrixStack()`, `new Color()`, or `String.format()` within `render()`, `render3D()`, or `render2D()`.
  - Use reusable static scratch buffers (`MemoryStack`, static vector pools).
- **Framebuffer Re-binding Overhead**:
  - Group 2D render batches to minimize `nvgBeginFrame` / `nvgEndFrame` context switches.
- **Launcher Coroutine Optimization**:
  - Ensure background instance scanning and process radar use `Dispatchers.IO` with bounded intervals and cancellation tokens.

### PHASE 4: FEATURE EXPANSION & GRANULAR SETTINGS
Expand and enrich the client with verified features:
1. **New Modules to Implement**:
   - `AutoTotemModule`: Dual offhand totem swap with smart health thresholds and crystal pop counter.
   - `AnchorMacroModule`: Responsive Respawn Anchor clicker with glowstone re-charge and explosive trigger.
   - `TracersModule`: 3D gradient vector lines pointing toward visible entities with friend/enemy color awareness.
   - `KillAuraModule` (or `TriggerBotModule`): Precise raycast target acquisition with weapon cooldown timing and critical hit synchronization.
2. **Deep Setting Additions**:
   - For all modules, introduce granular sliders (e.g. animation speed, smoothing alpha, jitter delays, distance thresholds) and color pickers.
   - Ensure every new setting is registered in the module constructor and properly handled in `ConfigManager.java`.

### PHASE 5: COMPILATION & VERIFICATION GATE
Execute the full clean build sequence:
```bash
./gradlew :bridge:build :client:compileJava :client:remapJar :agent:assemble :launcher:packageUberJarForCurrentOS
```
Ensure:
- Zero compile errors.
- Zero unresolved symbols or deprecated Yarn 1.21.11 API calls.
- Both `aether-agent.jar` and `AetherLauncher.jar` assemble cleanly.

### PHASE 6: STANDARDIZED HANDOFF REPORT GENERATION
Upon completing all improvements, write a comprehensive report to `BACKUP_IMPROVEMENTS_REPORT.md` following the template below:

```markdown
# BACKUP IMPROVEMENTS & AUDIT REPORT

## 1. EXECUTIVE SUMMARY
- Total files inspected: [COUNT]
- Total files modified: [COUNT]
- Defects resolved: [COUNT]
- Performance optimizations: [COUNT]
- New features/settings added: [COUNT]

## 2. DEFECT RESOLUTION LOG
### Defect #[ID]: [Title]
- **File & Lines**: `path/to/File.ext#L12-L45`
- **Root Cause**: [Explanation]
- **Solution Applied**: [Code snippet / diff explanation]
- **Verification Proof**: [How it was tested/verified]

## 3. PERFORMANCE OPTIMIZATIONS
- **Component**: [Name]
- **Before**: [Allocations/FPS impact]
- **After**: [Optimized pattern]

## 4. NEW MODULES & SETTINGS MANIFEST
- **Module**: `NewModuleName`
  - Description: ...
  - Settings:
    - `settingName` (Type, Default, Range): Description

## 5. COMPILATION & INTEGRATION VERIFICATION
- Gradle Tasks Verified:
  - `:bridge:build` -> SUCCESS
  - `:client:compileJava` -> SUCCESS
  - `:client:remapJar` -> SUCCESS
  - `:agent:assemble` -> SUCCESS
  - `:launcher:packageUberJarForCurrentOS` -> SUCCESS

## 6. PRIMARY AGENT MERGE DIRECTIVE
Step-by-step instructions for the Primary Agent to cherry-pick or merge these changes into `main` without merge conflicts.
```

---

## 3. EXECUTION INSTRUCTIONS FOR THE AUDIT AGENT
1. Switch to or create the working staging branch: `git checkout -b backup-staging`.
2. Execute Phases 1 through 5 with extreme care and rigor.
3. Write `BACKUP_IMPROVEMENTS_REPORT.md` into the repository root.
4. Commit changes: `git commit -am "feat(audit): comprehensive defect remediation and module overhaul"`.
5. Signal completion with the exact path to `BACKUP_IMPROVEMENTS_REPORT.md`.
