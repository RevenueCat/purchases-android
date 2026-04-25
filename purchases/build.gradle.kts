import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.revenuecat.public.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.poko)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localProperties.load(FileInputStream(localPropertiesFile))

// Resolves a property: Gradle -P flags (CI) take priority, then local.properties.
// Empty string from -P flags is treated as a valid value (e.g. empty ACTIVE_ENTITLEMENT_IDS_TO_VERIFY).
fun resolveProperty(name: String, default: String = ""): String {
    val projectProp = project.findProperty(name) as? String
    if (projectProp != null) return projectProp
    return localProperties.getProperty(name) ?: default
}

android {
    namespace = "com.revenuecat.purchases.api"

    buildFeatures {
        buildConfig = true
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
            type = "String",
            name = "SAMSUNG_IAP_SDK_VERSION",
            value = "\"${libs.versions.samsungIap.get()}\"",
        )

        packagingOptions.resources.excludes.addAll(
            listOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md"),
        )

        // Instrumentation test configuration — all environments passed so tests can run against any/all backends.
        // Each test class selects its environment via BasePurchasesIntegrationTest.environmentConfig.

        // Production environment
        testInstrumentationRunnerArguments["PRODUCTION_REVENUECAT_API_KEY"] =
            resolveProperty("REVENUECAT_API_KEY")
        testInstrumentationRunnerArguments["PRODUCTION_GOOGLE_PURCHASE_TOKEN"] =
            resolveProperty("GOOGLE_PURCHASE_TOKEN")
        testInstrumentationRunnerArguments["PRODUCTION_PRODUCT_ID_TO_PURCHASE"] =
            resolveProperty("PRODUCT_ID_TO_PURCHASE")
        testInstrumentationRunnerArguments["PRODUCTION_BASE_PLAN_ID_TO_PURCHASE"] =
            resolveProperty("BASE_PLAN_ID_TO_PURCHASE")
        testInstrumentationRunnerArguments["PRODUCTION_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"] =
            resolveProperty("ACTIVE_ENTITLEMENT_IDS_TO_VERIFY")

        // Load Shedder environment (shared keys for US-East-1 and US-East-2)
        testInstrumentationRunnerArguments["LOAD_SHEDDER_REVENUECAT_API_KEY"] =
            resolveProperty("LOAD_SHEDDER_REVENUECAT_API_KEY")
        testInstrumentationRunnerArguments["LOAD_SHEDDER_GOOGLE_PURCHASE_TOKEN"] =
            resolveProperty("LOAD_SHEDDER_GOOGLE_PURCHASE_TOKEN")
        testInstrumentationRunnerArguments["LOAD_SHEDDER_PRODUCT_ID_TO_PURCHASE"] =
            resolveProperty("LOAD_SHEDDER_PRODUCT_ID_TO_PURCHASE")
        testInstrumentationRunnerArguments["LOAD_SHEDDER_BASE_PLAN_ID_TO_PURCHASE"] =
            resolveProperty("LOAD_SHEDDER_BASE_PLAN_ID_TO_PURCHASE")
        testInstrumentationRunnerArguments["LOAD_SHEDDER_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"] =
            resolveProperty("LOAD_SHEDDER_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY")

        // Custom Entitlement Computation environment
        testInstrumentationRunnerArguments["CEC_REVENUECAT_API_KEY"] =
            resolveProperty("CUSTOM_ENTITLEMENT_COMPUTATION_REVENUECAT_API_KEY")
        testInstrumentationRunnerArguments["CEC_GOOGLE_PURCHASE_TOKEN"] =
            resolveProperty("CUSTOM_ENTITLEMENT_COMPUTATION_GOOGLE_PURCHASE_TOKEN")
        testInstrumentationRunnerArguments["CEC_PRODUCT_ID_TO_PURCHASE"] =
            resolveProperty("CUSTOM_ENTITLEMENT_COMPUTATION_PRODUCT_ID_TO_PURCHASE")
        testInstrumentationRunnerArguments["CEC_BASE_PLAN_ID_TO_PURCHASE"] =
            resolveProperty("CUSTOM_ENTITLEMENT_COMPUTATION_BASE_PLAN_ID_TO_PURCHASE")
        testInstrumentationRunnerArguments["CEC_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"] =
            resolveProperty("CUSTOM_ENTITLEMENT_COMPUTATION_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY")

        // Shared
        testInstrumentationRunnerArguments["TEST_PROXY_URL"] = resolveProperty("TEST_PROXY_URL")

        // Optional package filter for running a subset of tests (used by CI).
        // e.g. -PTEST_PACKAGE_FILTER=com.revenuecat.purchases.integration.production
        val testPackageFilter = resolveProperty("TEST_PACKAGE_FILTER")
        if (testPackageFilter.isNotEmpty()) {
            testInstrumentationRunnerArguments["package"] = testPackageFilter
        }
    }

    testOptions {
        unitTests.all {
            // Pass test keys as JVM system properties for backend integration tests
            it.systemProperty("BACKEND_INTEGRATION_API_KEY", resolveProperty("BACKEND_INTEGRATION_API_KEY"))
            it.systemProperty(
                "BACKEND_INTEGRATION_LOAD_SHEDDER_API_KEY",
                resolveProperty("BACKEND_INTEGRATION_LOAD_SHEDDER_API_KEY"),
            )
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
    implementation(libs.purchases.core)
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
