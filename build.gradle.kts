plugins {
    base
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.emerge) apply false
    alias(libs.plugins.poko) apply false
    alias(libs.plugins.metalava) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependencyGraph)
    //    Removing from here gives an error
    //    The request for this plugin could not be satisfied because the plugin is already on the classpath with an
    //    unknown version, so compatibility cannot be checked.
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose)
    detektPlugins(libs.detekt.libraries)
    detektPlugins(project(":detekt-rules"))
}

// Aggregate docs from every module that applies the Dokka plugin (currently done by revenuecat-public-library),
// so new library modules are documented without having to remember to list them here.
val dokkaPluginId = libs.plugins.dokka.get().pluginId
subprojects {
    plugins.withId(dokkaPluginId) {
        rootProject.dependencies.add("dokka", project)
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(file("docs/${project.property("VERSION_NAME")}"))
        includes.from("README.md")
    }
}

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektAll") {
    description = "Runs over the whole codebase without the startup overhead for each module."
    buildUponDefaultConfig = true
    autoCorrect = true
    parallel = true
    setSource(files(rootDir))
    include("**/*.kt", "**/*.kts")
    exclude(
        "**/build/**",
        "**/test/**/*.kt",
        "**/testDefaults/**/*.kt",
        "**/testCustomEntitlementComputation/**/*.kt",
    )
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("build/reports/detekt/detekt.xml"))
        html.required.set(false)
        txt.required.set(false)
    }
}

tasks.register<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>("detektAllBaseline") {
    description = "Overrides the current top-level baseline with issues found during this run."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
    include("**/*.kt", "**/*.kts")
    exclude(
        "**/build/**",
        "**/test/**/*.kt",
        "**/testDefaults/**/*.kt",
        "**/testCustomEntitlementComputation/**/*.kt",
    )
}
