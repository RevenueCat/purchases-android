import java.io.File
import java.net.URL

buildscript {
    extra["compileVersion"] = 35
    extra["minVersion"] = 21
}

plugins {
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
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

val samsungIapVersion = libs.versions.samsungIap.get()
val samsungIapFileName = "samsung-iap-$samsungIapVersion.aar"
val samsungIapDestFile = rootProject.file("libs/$samsungIapFileName")

tasks.register("getSamsungIapSdk") {
    val downloadUrl = System.getenv("SAMSUNG_IAP_SDK_URL").orEmpty()

    inputs.property("downloadURL", downloadUrl)
    inputs.property("fileToExtract", samsungIapFileName)
    outputs.file(samsungIapDestFile)

    doLast {
        if (samsungIapDestFile.exists()) {
            return@doLast
        }
        if (downloadUrl.isBlank()) {
            throw GradleException("SAMSUNG_IAP_SDK_URL is not set")
        }

        logger.lifecycle("Downloading Samsung IAP SDK")
        samsungIapDestFile.parentFile.mkdirs()

        val downloadFile = File(temporaryDir, "download.zip")
        URL(downloadUrl).openStream().use { input ->
            downloadFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val downloadedSizeMb = downloadFile.length().toDouble() / (1024 * 1024)
        logger.lifecycle("Downloaded Samsung IAP SDK archive: %.2f MB".format(downloadedSizeMb))

        if (downloadUrl.lowercase().endsWith(".zip")) {
            logger.lifecycle("Samsung IAP SDK download detected as ZIP. Extracting $samsungIapFileName from archive.")
            project.copy {
                from(
                    zipTree(downloadFile)
                        .matching { include("**/$samsungIapFileName") }
                        .singleFile,
                )
                into(samsungIapDestFile.parentFile)
            }
        } else {
            logger.lifecycle("Samsung IAP SDK download detected as non-ZIP. Copying file directly as AAR.")
            downloadFile.copyTo(samsungIapDestFile, overwrite = true)
        }
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

tasks.named<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaHtmlMultiModule") {
    outputDirectory.set(file("docs/${project.property("VERSION_NAME")}"))
    includes.from("README.md")
}
