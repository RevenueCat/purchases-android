import org.gradle.api.GradleException

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

val isCiBuild = providers.environmentVariable("CI").orNull.equals("true", ignoreCase = true)

if (isCiBuild) {
    val samsungIapVersion = libs.versions.samsungIap.get()
    val samsungIapFilename = "samsung-iap-$samsungIapVersion.aar"
    val samsungIapMavenUrl = providers.environmentVariable("SAMSUNG_IAP_MAVEN_URL").orNull
        ?: throw GradleException("SAMSUNG_IAP_MAVEN_URL must be set when CI=true.")
    val ghPackagesUser = providers.environmentVariable("READ_GH_PACKAGES_USER").orNull
        ?: throw GradleException("READ_GH_PACKAGES_USER must be set when CI=true.")
    val ghPackagesPat = providers.environmentVariable("READ_GH_PACKAGES_PAT").orNull
        ?: throw GradleException("READ_GH_PACKAGES_PAT must be set when CI=true.")

    val samsungIapDownload by configurations.creating {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }

    repositories {
        maven {
            url = uri(samsungIapMavenUrl)
            credentials {
                username = ghPackagesUser
                password = ghPackagesPat
            }
        }
    }

    dependencies {
        add(samsungIapDownload.name, "com.samsung.iap:samsung-iap:$samsungIapVersion@aar")
    }

    tasks.register<Sync>("downloadSamsungIapAar") {
        group = "build setup"
        description = "Downloads the Samsung IAP AAR into the root libs directory when running in CI."

        doFirst {
            val resolvedFiles = samsungIapDownload.resolve()
            if (resolvedFiles.size != 1) {
                throw GradleException(
                    "Expected exactly one Samsung IAP AAR artifact, but resolved ${resolvedFiles.size}: " +
                        resolvedFiles.joinToString { it.name },
                )
            }
        }

        from(samsungIapDownload)
        into(layout.projectDirectory.dir("libs"))
        rename { samsungIapFilename }

        doLast {
            val outputFile = layout.projectDirectory.file("libs/$samsungIapFilename").asFile
            if (!outputFile.exists()) {
                throw GradleException("Samsung IAP AAR was not copied to ${outputFile.path}.")
            }
            if (outputFile.length() <= 0L) {
                throw GradleException("Samsung IAP AAR at ${outputFile.path} is empty.")
            }
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
