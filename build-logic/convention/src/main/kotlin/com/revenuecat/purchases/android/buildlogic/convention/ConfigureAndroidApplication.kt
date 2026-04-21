package com.revenuecat.purchases.android.buildlogic.convention

import com.android.build.gradle.BaseExtension
import com.revenuecat.purchases.android.buildlogic.ktx.getVersion
import com.revenuecat.purchases.android.buildlogic.ktx.versionCatalog
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureAndroidApplication() {
    val libs = versionCatalog
    val compileSdkVersion = libs.getVersion("android-compileSdk").toInt()
    val minSdkVersion = libs.getVersion("android-minSdk").toInt()
    val targetSdkVersion = libs.getVersion("android-targetSdk").toInt()

    extensions.configure<BaseExtension> {
        compileSdkVersion(compileSdkVersion)

        defaultConfig {
            minSdk = minSdkVersion
            targetSdk = targetSdkVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        buildFeatures.buildConfig = true
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
}
