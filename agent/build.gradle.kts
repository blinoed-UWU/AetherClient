import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("java")
}

group = "dev.aetherclient"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    create("transformer") {
        java.srcDir("src/transformer/java")
    }
}

val asmPayload by configurations.creating
val nanovgJar by configurations.creating
val nanovgNatives by configurations.creating
val bridgeJarConfig by configurations.creating

dependencies {
    "transformerImplementation"("org.ow2.asm:asm:9.7.1")
    "transformerImplementation"("org.ow2.asm:asm-commons:9.7.1")
    "transformerImplementation"("org.ow2.asm:asm-tree:9.7.1")

    asmPayload("org.ow2.asm:asm:9.7.1")
    asmPayload("org.ow2.asm:asm-commons:9.7.1")
    asmPayload("org.ow2.asm:asm-tree:9.7.1")

    nanovgJar("org.lwjgl:lwjgl-nanovg:3.3.3") {
        isTransitive = false
    }
    nanovgNatives("org.lwjgl:lwjgl-nanovg:3.3.3:natives-linux") { isTransitive = false }
    nanovgNatives("org.lwjgl:lwjgl-nanovg:3.3.3:natives-linux-arm64") { isTransitive = false }
    nanovgNatives("org.lwjgl:lwjgl-nanovg:3.3.3:natives-windows") { isTransitive = false }
    nanovgNatives("org.lwjgl:lwjgl-nanovg:3.3.3:natives-macos") { isTransitive = false }
    nanovgNatives("org.lwjgl:lwjgl-nanovg:3.3.3:natives-macos-arm64") { isTransitive = false }

    bridgeJarConfig(project(":bridge"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(sourceSets["transformer"].output)
    testImplementation("org.ow2.asm:asm:9.7.1")
    testImplementation("org.ow2.asm:asm-commons:9.7.1")
    testImplementation("org.ow2.asm:asm-tree:9.7.1")
    testImplementation("org.ow2.asm:asm-util:9.7.1")
    testImplementation(project(":bridge"))
}

val transformerJar by tasks.registering(Jar::class) {
    archiveBaseName.set("aether-transformer")
    from(sourceSets["transformer"].output)
}

val stagePayloads by tasks.registering(Sync::class) {
    dependsOn(":bridge:jar", ":client:remapJar", transformerJar)

    val clientJarTask = project(":client").tasks.named<AbstractArchiveTask>("remapJar")
    val bridgeJarTask = project(":bridge").tasks.named<Jar>("jar")

    into(layout.buildDirectory.dir("staged-payload"))

    from(bridgeJarTask.flatMap { it.archiveFile }) {
        into("payload")
        rename { "aether-bridge.jar" }
    }
    from(clientJarTask.flatMap { it.archiveFile }) {
        into("payload")
        rename { "aether-client.jar" }
    }
    from(transformerJar.flatMap { it.archiveFile }) {
        into("payload")
        rename { "aether-transformer.jar" }
    }
    from(nanovgJar) {
        into("payload")
        rename { "lwjgl-nanovg.jar" }
    }
    from(asmPayload) {
        into("payload/asm")
    }
    from(nanovgNatives) {
        into("payload/natives")
    }
}

tasks.named<Jar>("jar") {
    dependsOn(stagePayloads)
    archiveFileName.set("aether-agent.jar")

    from(layout.buildDirectory.dir("staged-payload"))

    manifest {
        attributes(
            "Agent-Class" to "dev.aetherclient.agent.AetherAgentMain",
            "Premain-Class" to "dev.aetherclient.agent.AetherAgentMain",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}

val copyAgentForLoader by tasks.registering(Copy::class) {
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.projectDirectory.dir("client/build/libs"))
    rename { "aether-agent.jar" }
}

val copyAgentToLauncherResources by tasks.registering(Copy::class) {
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.projectDirectory.dir("launcher/src/desktopMain/resources/payload"))
    rename { "aether-agent.jar" }
}

tasks.named("assemble") {
    dependsOn(copyAgentForLoader, copyAgentToLauncherResources)
}

tasks.named("build") {
    dependsOn(copyAgentForLoader, copyAgentToLauncherResources)
}

tasks.test {
    useJUnitPlatform()
}
