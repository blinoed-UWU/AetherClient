package dev.aetherclient.launcher

import dev.aetherclient.launcher.core.bootstrap.LoaderBootstrapper
import dev.aetherclient.launcher.core.inject.JvmInjector
import dev.aetherclient.launcher.core.launch.InstanceManager
import dev.aetherclient.launcher.core.model.LoaderSettings
import dev.aetherclient.launcher.core.model.PaletteStyleOption
import dev.aetherclient.launcher.core.storage.LauncherStateStore
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LauncherCoreTest {
    @Test
    fun `settings serialization roundtrip preserves theme and injector flags`() {
        val settings = LoaderSettings(
            darkMode = false,
            seedColorHex = 0xFF10B981,
            paletteStyle = PaletteStyleOption.VIBRANT,
            autoInject = true,
            minimizeAfterInject = true,
            showAllJvmProcesses = false,
            customAgentPath = "/tmp/custom-agent.jar",
            selectedInstanceId = "optimization 1.21.11"
        )
        val encoded = LauncherStateStore.json.encodeToString(settings)
        val decoded = LauncherStateStore.json.decodeFromString<LoaderSettings>(encoded)
        assertEquals(settings, decoded)
    }

    @Test
    fun `jdk attach api is available in runtime`() {
        assertTrue(JvmInjector.isAttachAvailable())
    }

    @Test
    fun `resolvePayloadPath respects custom agent path`() {
        val resolved = LoaderBootstrapper.resolvePayloadPath("/tmp/my-aether-agent.jar")
        assertEquals(Paths.get("/tmp/my-aether-agent.jar"), resolved)
    }

    @Test
    fun `detects Minecraft Fabric wrapped by PrismLauncher or FreesmLauncher EntryPoint`() {
        val info = JvmInjector.classifyProcess(
            pid = "65884",
            rawDisplay = "org.prismlauncher.EntryPoint -Djava.library.path=/home/mark/.local/share/FreesmLauncher/instances/optimization 1.21.11/natives",
            cmdline = "/path/java -cp /libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar:/libraries/com/mojang/minecraft/1.21.11/minecraft-1.21.11-client.jar org.prismlauncher.EntryPoint"
        )
        assertTrue(info != null && info.isMinecraft)
        assertEquals("Minecraft • Fabric", info.title)
        assertEquals("PID 65884 • optimization 1.21.11", info.subtitle)
    }

    @Test
    fun `patchInstanceCfgForAgent adds OverrideJavaArgs and preserves existing flags`() {
        val original = """
            [General]
            InstanceType=OneSix
            OverrideJavaArgs=false
            JvmArgs=-Xmx4G -XX:+UseG1GC -javaagent:/old/aether-agent.jar
            name=optimization 1.21.11
        """.trimIndent() + "\n"

        val patched = InstanceManager.patchInstanceCfgForAgent(
            original,
            Paths.get("/home/mark/.aether-client/payloads/aether-agent.jar")
        )

        assertTrue(patched.contains("OverrideJavaArgs=true"))
        assertFalse(patched.contains("/old/aether-agent.jar"))
        assertTrue(
            patched.contains(
                "JvmArgs=-javaagent:/home/mark/.aether-client/payloads/aether-agent.jar -Xmx4G -XX:+UseG1GC"
            )
        )
    }

    @Test
    fun `parseInstanceDir detects 1_21_11 Fabric compatibility from mmc-pack`() {
        val dir = Files.createTempDirectory("aether-inst-test")
        try {
            Files.writeString(dir.resolve("instance.cfg"), "[General]\nname=My Fabric 1.21.11\n")
            Files.writeString(
                dir.resolve("mmc-pack.json"),
                """{"components":[{"uid":"net.minecraft","version":"1.21.11"},{"uid":"net.fabricmc.fabric-loader","version":"0.19.5"}]}"""
            )
            val parsed = InstanceManager.parseInstanceDir(dir, "FreesmLauncher", "/usr/bin/freesmlauncher")
            assertTrue(parsed != null)
            assertEquals("My Fabric 1.21.11", parsed.name)
            assertEquals("1.21.11", parsed.mcVersion)
            assertTrue(parsed.isFabric)
            assertTrue(parsed.isCompatible)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `classifyProcess detects hasPreAgent only when aether javaagent is present`() {
        val withPreAgent = JvmInjector.classifyProcess(
            pid = "77701",
            rawDisplay = "org.prismlauncher.EntryPoint",
            cmdline = "/usr/lib/jvm/java-21/bin/java -javaagent:/home/mark/.aether-client/payloads/aether-agent.jar -cp /libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar org.prismlauncher.EntryPoint"
        )
        assertTrue(withPreAgent != null && withPreAgent.isMinecraft)
        assertTrue(withPreAgent.hasPreAgent)

        val withoutPreAgent = JvmInjector.classifyProcess(
            pid = "77702",
            rawDisplay = "org.prismlauncher.EntryPoint",
            cmdline = "/usr/lib/jvm/java-21/bin/java -javaagent:/opt/other-profiler.jar -cp /libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar org.prismlauncher.EntryPoint"
        )
        assertTrue(withoutPreAgent != null && withoutPreAgent.isMinecraft)
        assertFalse(withoutPreAgent.hasPreAgent)
    }

    @Test
    fun `patchInstanceCfgForAgent and stripExistingAetherAgent handle spaces and unrelated agents`() {
        val dir = Files.createTempDirectory("aether path with spaces")
        try {
            val agentWithSpaces = dir.resolve("aether-agent.jar")
            val original = """
                [General]
                OverrideJavaArgs=false
                JvmArgs="-javaagent:/old dir/aether-agent.jar" -javaagent:"/other dir/custom-aether.jar" -javaagent:/opt/keep-me.jar -Xmx6G
            """.trimIndent() + "\n"

            val patched = InstanceManager.patchInstanceCfgForAgent(original, agentWithSpaces)
            val expectedFlag = "\"-javaagent:${agentWithSpaces.toAbsolutePath().normalize()}\""

            assertTrue(patched.contains("OverrideJavaArgs=true"))
            assertFalse(patched.contains("/old dir/aether-agent.jar"))
            assertFalse(patched.contains("/other dir/custom-aether.jar"))
            assertTrue(patched.contains("-javaagent:/opt/keep-me.jar"))
            assertTrue(patched.contains("-Xmx6G"))
            assertTrue(patched.contains("JvmArgs=$expectedFlag -javaagent:/opt/keep-me.jar -Xmx6G"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `resolvePayloadPath ignores syntactically invalid custom path without throwing`() {
        val fallback = LoaderBootstrapper.resolvePayloadPath("/tmp/bad\u0000path/aether-agent.jar")
        assertTrue(fallback.fileName.toString() == "aether-agent.jar")
    }
}
