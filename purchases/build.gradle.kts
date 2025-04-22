import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.poko)
    alias(libs.plugins.metalava)
}

apply(from = "${rootProject.projectDir}/library.gradle")

android {
    namespace = "com.revenuecat.purchases.api"

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "apis"

    productFlavors {
        create("defaults") {
            dimension = "apis"
            isDefault = true
        }
        create("customEntitlementComputation") {
            dimension = "apis"
        }
    }

    defaultConfig {
        testApplicationId = obtainTestApplicationId()
        testBuildType = obtainTestBuildType()

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

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }

    if (name.contains("UnitTest") || name.contains("AndroidTest")) {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=com.revenuecat.purchases.InternalRevenueCatAPI")
        }
    }
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
    implementation(libs.tink)
    implementation(libs.playServices.ads.identifier)
    api(libs.billing)

    compileOnly(libs.amazon.appstore.sdk)
    compileOnly(libs.coil.base)

    dokkaPlugin(project(":dokka-hide-internal"))

    testImplementation(libs.coil.base)
    testImplementation(libs.bundles.test)
    testImplementation(libs.billing)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.amazon.appstore.sdk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.playServices.ads.identifier)
    testImplementation(libs.testJUnitParams)

    androidTestImplementation(libs.androidx.appcompat)
    androidTestImplementation(libs.androidx.lifecycle.runtime.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.assertJ)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)
}

tasks.dokkaHtmlPartial.configure {
    dokkaSourceSets {
        named("customEntitlementComputation") {
            suppress.set(true)
        }
        named("defaults") {
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
