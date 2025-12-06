import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("revenuecat-public-library")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.poko)
}

android {
    namespace = "com.revenuecat.purchases.ui.revenuecatui"

    // billingclient dimension is added for bc7/bc8 support
    flavorDimensions += "billingclient"
    productFlavors {
        create("bc8") {
            dimension = "billingclient"
            isDefault = true
        }
        create("bc7") {
            dimension = "billingclient"
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "PROJECT_DIR", "\"$projectDir\"")
        }
    }

    sourceSets {
        getByName("debug") {
            resources.srcDirs("src/debug/resources", "../../upstream/paywall-preview-resources/resources")
        }
    }

    defaultConfig {
        minSdk = 24 // MeasureFormat requires API 24
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<Test> {
    // This is needed because of an incompatibility between Robolectric and Paparazzi:
    // https://github.com/cashapp/paparazzi/issues/1979
    forkEvery = 1
    maxParallelForks = 5
}

metalava {
    hiddenAnnotations.add("com.revenuecat.purchases.InternalRevenueCatAPI")
    arguments.addAll(listOf("--hide", "ReferencesHidden"))
    excludedSourceSets.setFrom(
        "src/test",
        "src/testDefaults",
        "src/testCustomEntitlementComputation",
        "src/androidTest",
        "src/androidTestDefaults",
        "src/androidTestCustomEntitlementComputation",
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        if (project.findProperty("revenuecat.enableComposeCompilerReports") == "true") {
            val composeMetricsDir = "${project.buildDir.absolutePath}/compose_metrics"
            freeCompilerArgs.addAll(
                listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeMetricsDir",
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeMetricsDir",
                ),
            )
        }
    }
}

dependencies {
    api(project(":purchases"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3Adaptive)
    implementation(libs.compose.constraintlayout)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui.google.fonts)

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.window)
    implementation(libs.window.core)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    implementation(libs.commonmark)
    implementation(libs.commonmark.strikethrough)

    compileOnly(libs.emerge.snapshots.runtime)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.test.compose.manifest)

    testImplementation(libs.bundles.test)
    testImplementation(libs.coil.test)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.androidx.test.compose)
    testImplementation(libs.androidx.test.compose.manifest)
    testImplementation(libs.hamcrest.core)

    androidTestImplementation(libs.assertJ)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)
    androidTestImplementation(libs.androidx.test.compose)

    baselineProfile(project(":baselineprofile"))
}

tasks.dokkaHtmlPartial.configure {
    dokkaSourceSets {
        named("defaultsBc7") {
            suppress.set(true)
        }
        named("defaultsBc8") {
            dependsOn("main")
        }
        named("main") {
            reportUndocumented.set(true)
            includeNonPublic.set(false)
            skipDeprecated.set(true)

            externalDocumentationLink {
                url.set(
                    uri("https://developer.android.com/reference/package-list").toURL(),
                )
            }
            sourceLink {
                localDirectory.set(
                    file("src/main/kotlin"),
                )
                remoteUrl.set(
                    uri(
                        "https://github.com/revenuecat/purchases-android/blob/main/ui/revenuecatui/src/main/kotlin",
                    ).toURL(),
                )
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// Opt-in to InternalRevenueCatAPI for the entire module. This avoids having to explicitly opt in at every call site
// separately. Lint is handled by ui/revenuecatui/lint.xml.
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=com.revenuecat.purchases.InternalRevenueCatAPI")
    }
}

baselineProfile {
    mergeIntoMain = true
    baselineProfileOutputDir = "."
    filter {
        include("com.revenuecat.purchases.ui.revenuecatui.**")
    }
}
