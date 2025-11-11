package com.revenuecat.purchases.android.buildlogic.convention

import com.android.build.gradle.LibraryExtension
import com.revenuecat.purchases.android.buildlogic.ktx.getVersion
import com.revenuecat.purchases.android.buildlogic.ktx.versionCatalog
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

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

    extensions.configure<LibraryExtension> {
        compileSdk = compileSdkVersion

        defaultConfig {
            minSdk = obtainMinSdkVersion()
            targetSdk = targetSdkVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            consumerProguardFiles("consumer-rules.pro")
        }

        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.all {
                it.maxHeapSize = "1024m"
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
            val kotlinLanguageVersion = libs.getVersion("kotlinLanguage")
            languageVersion.set(
                org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(kotlinLanguageVersion),
            )
            apiVersion.set(
                org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(kotlinLanguageVersion),
            )
        }
    }
}
