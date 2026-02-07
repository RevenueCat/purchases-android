import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("revenuecat-public-library")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.poko)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localProperties.load(FileInputStream(localPropertiesFile))

android {
    namespace = "com.revenuecat.purchases.api"

    buildFeatures {
        buildConfig = true
        aidl = true
    }

    // billingclient dimension is added for bc7/bc8 support
    flavorDimensions += "billingclient"

    productFlavors {
        create("customEntitlementComputation") {
            dimension = "apis"
        }
        create("bc8") {
            dimension = "billingclient"
            isDefault = true
            buildConfigField(
                type = "String",
                name = "BILLING_CLIENT_VERSION",
                value = "\"${libs.versions.bc8.get()}\"",
            )
        }
        create("bc7") {
            dimension = "billingclient"
            buildConfigField(
                type = "String",
                name = "BILLING_CLIENT_VERSION",
                value = "\"${libs.versions.bc7.get()}\"",
            )
        }
    }

    defaultConfig {
        testApplicationId = obtainTestApplicationId()
        testBuildType = obtainTestBuildType()

        buildConfigField(
            type = "boolean",
            name = "ENABLE_EXTRA_REQUEST_LOGGING",
            value = (localProperties["ENABLE_EXTRA_REQUEST_LOGGING"] as? String ?: "false"),
        )
        buildConfigField(
            type = "boolean",
            name = "ENABLE_QUERY_PURCHASE_HISTORY_AIDL",
            value = (localProperties["ENABLE_QUERY_PURCHASE_HISTORY_AIDL"] as? String ?: "true"),
        )

        packagingOptions.resources.excludes.addAll(
            listOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md"),
        )
    }

    testOptions {
        unitTests.all {
            if (project.hasProperty("RUN_INTEGRATION_TESTS")) {
                it.include("com/revenuecat/purchases/backend_integration_tests/**")
            } else {
                it.exclude("com/revenuecat/purchases/backend_integration_tests/**")
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        if (variant.productFlavors.any { it.second == "free" }) {
            tasks.register("customTaskForFree${variant.name.capitalize()}") {
                doLast {
                    println("Running for FREE flavor: ${variant.name}")
                }
            }
        }
    }
}

val variantName = project.gradle.startParameter.taskNames.joinToString(" ")

metalava {
    val excludeSourceSets = mutableListOf(
        "src/test",
        "src/testDefaults",
        "src/testCustomEntitlementComputation",
        "src/androidTest",
        "src/androidTestDefaults",
        "src/androidTestCustomEntitlementComputation",
    )

    val name = if (variantName.lowercase().contains("defaults")) {
        excludeSourceSets.add("src/customEntitlementComputation/kotlin")
        if (variantName.lowercase().contains("bc8")) {
            "api-defauts.txt"
        } else if (variantName.lowercase().contains("bc7")) {
            "api-defaults-bc7.txt"
        } else {
            "api-defaults-unknown.txt"
        }
    } else if (variantName.lowercase().contains("entitlement")) {
        excludeSourceSets.add("src/defaults/kotlin")
        "api-entitlement.txt"
    } else {
        "unknown.txt"
    }

    filename.set(name)
    hiddenAnnotations.add("com.revenuecat.purchases.InternalRevenueCatAPI")
    arguments.addAll(listOf("--hide", "ReferencesHidden"))
    excludedSourceSets.setFrom(excludeSourceSets)
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }

    if (name.contains("UnitTest") || name.contains("AndroidTest")) {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=com.revenuecat.purchases.InternalRevenueCatAPI")
            freeCompilerArgs.add("-opt-in=com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI")
        }
    }
}

tasks.withType<Test> {
    // Disabling verification in tests until Amazon publishes a version of their SDK compiled with a modern JDK.
    jvmArgs("-noverify")
}

fun obtainTestApplicationId(): String =
    if (project.hasProperty("testApplicationId")) {
        project.properties["testApplicationId"] as String
    } else {
        "com.revenuecat.purchases.integrationTest"
    }

fun obtainTestBuildType(): String =
    if (project.hasProperty("testBuildType")) {
        project.properties["testBuildType"] as String
    } else {
        "debug"
    }

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.androidx.core)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.google.blockstore)
    implementation(libs.tink)
    implementation(libs.playServices.ads.identifier)
    implementation(libs.coroutines.core)
    implementation("com.revenuecat.purchases:purchases-core:0.0.0-rust-SNAPSHOT")
    "bc8Api"(libs.billing.bc8)
    "bc7Api"(libs.billing.bc7)

    compileOnly(libs.compose.annotations)
    compileOnly(libs.amazon.appstore.sdk)
    compileOnly(libs.coil.base)

    debugImplementation(libs.androidx.annotation.experimental)

    dokkaPlugin(project(":dokka-hide-internal"))

    testImplementation(libs.coil.base)
    testImplementation(libs.bundles.test)
    "testBc8Implementation"(libs.billing.bc8)
    "testBc7Implementation"(libs.billing.bc7)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.amazon.appstore.sdk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.playServices.ads.identifier)
    testImplementation(libs.testJUnitParams)

    androidTestImplementation(libs.androidx.appcompat)
    androidTestImplementation(libs.androidx.lifecycle.runtime.ktx)
    androidTestImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.assertJ)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)
    androidTestImplementation(libs.leakcanary.android.instrumentation)

    baselineProfile(project(":baselineprofile"))
    testImplementation(kotlin("test"))
}

tasks.dokkaHtmlPartial.configure {
    dokkaSourceSets {
        named("customEntitlementComputationBc8") {
            suppress.set(true)
        }
        named("customEntitlementComputationBc7") {
            suppress.set(true)
        }
        named("defaultsBc7") {
            suppress.set(true)
        }
        named("defaultsBc8") {
            dependsOn("main")
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
                    uri("https://github.com/revenuecat/purchases-android/blob/main/purchases/src/main/kotlin").toURL(),
                )
                remoteLineSuffix.set("#L")
            }
            sourceLink {
                localDirectory.set(
                    file("src/main/java"),
                )
                remoteUrl.set(
                    uri("https://github.com/revenuecat/purchases-android/blob/main/public/src/main/java").toURL(),
                )
                remoteLineSuffix.set("#L")
            }
        }
        named("main") {
            reportUndocumented.set(true)
            includeNonPublic.set(false)
            skipDeprecated.set(true)

            // This package exclusively contains symbols annotated with @InternalRevenueCatAPI, for which no
            // documentation is generated due to our dokka-hide-internal plugin. However, by default Dokka still
            // generates an empty page for the package. This avoids that.
            perPackageOption {
                matchingRegex.set("com\\.revenuecat\\.purchases\\.paywalls\\.components.*")
                suppress.set(true)
            }
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
                        "https://github.com/revenuecat/purchases-android/blob/main/purchases/src/main/kotlin",
                    ).toURL(),
                )
                remoteLineSuffix.set("#L")
            }
            sourceLink {
                localDirectory.set(file("src/main/java"))
                remoteUrl.set(
                    uri("https://github.com/revenuecat/purchases-android/blob/main/public/src/main/java").toURL(),
                )
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// Remove afterEvaluate
// after https://github.com/Kotlin/kotlinx-kover/issues/362 is fixed
afterEvaluate {
    dependencies {
        add("kover", project(":feature:amazon"))
    }
}

baselineProfile {
    mergeIntoMain = true
    baselineProfileOutputDir = "."
    filter {
        include("com.revenuecat.purchases.**")
        exclude("com.revenuecat.purchases.ui.revenuecatui.**")
    }
}
