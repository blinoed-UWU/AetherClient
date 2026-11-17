plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
}

/*
 * Aggregated quality gate.
 *
 * The gate is assembled after all projects have been evaluated, so it can
 * depend on whatever verification tasks each subproject actually registers.
 * A missing plugin (e.g teargdown no `jacoco`) simply contributes no task.
 */
gradle.projectsEvaluated {
    val gate = rootProject.tasks.register("qualityGate") {
        group = "verification"
        description = "Runs every test, static-analysis, and coverage check across all modules."
    }
    subprojects.forEach { sub ->
        val deps = listOf("check", "test", "checkstyleMain", "checkstyleTest", "jacocoTestReport")
            .filter { sub.tasks.names.contains(it) }
            .map { sub.tasks.named(it) }
        if (deps.isNotEmpty()) {
            gate.configure { dependsOn(deps) }
        }
    }
}
