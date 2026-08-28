package com.revenuecat.purchases.android.buildlogic.convention

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.HasUnitTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.revenuecat.purchases.android.buildlogic.ktx.getVersion
import com.revenuecat.purchases.android.buildlogic.ktx.versionCatalog
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

internal fun Project.configureAndroidLibrary() {
    val libs = versionCatalog
    val compileSdkVersion = libs.getVersion("android-compileSdk").toInt()
    val minSdkVersion = libs.getVersion("android-minSdk").toInt()
    val targetSdkVersion = libs.getVersion("android-targetSdk").toInt()

    fun obtainMinSdkVersion(): Int {
        var result = minSdkVersion
        if (project.hasProperty("minSdkVersion")) {
            result = project.property("minSdkVersion").toString().toInt()
        }
        return result
    }

    extensions.configure<LibraryAndroidComponentsExtension> {
        beforeVariants { variantBuilder ->
            (variantBuilder as HasUnitTestBuilder).enableUnitTest = true
        }
    }

    extensions.configure<LibraryExtension> {
        compileSdk = compileSdkVersion

        defaultConfig {
            minSdk = obtainMinSdkVersion()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            consumerProguardFiles("consumer-rules.pro")
            // AGP 9 defaults this to compileSdk, tightening the published floor from 1 to 36.
            aarMetadata.minCompileSdk = 1
        }

        // AGP 9 removes targetSdk from a library's defaultConfig.
        testOptions {
            targetSdk = targetSdkVersion
            unitTests.isIncludeAndroidResources = true
            unitTests.all {
                it.maxHeapSize = "1024m"
            }
        }

        lint {
            targetSdk = targetSdkVersion
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        // Apply explicit API mode only to main (non-test) compilations
        target.compilations.configureEach {
            if (!name.contains("test", ignoreCase = true)) {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.add("-Xexplicit-api=strict")
                    }
                }
            }
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            // Emits interface default bodies as real JVM default methods, so adding a method to a
            // public interface is not source-breaking for Java implementors. Keeps DefaultImpls: ABI-safe.
            freeCompilerArgs.add("-Xjvm-default=all-compatibility")
            val kotlinLanguageVersion = libs.getVersion("kotlinLanguage")
            languageVersion.set(
                KotlinVersion.fromVersion(kotlinLanguageVersion),
            )
            apiVersion.set(
                KotlinVersion.fromVersion(kotlinLanguageVersion),
            )
        }
    }
}
