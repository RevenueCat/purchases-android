import java.util.Properties

plugins {
    alias(libs.plugins.revenuecat.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.revenuecat.sample.admob.nextgen"

    defaultConfig {
        applicationId = "com.revenuecat.sample.admob.nextgen"
        minSdk = 24
        versionCode = 1
        versionName = "1.0"

        fun stringField(name: String, defaultValue: String = "") {
            val value = localProperties.getProperty(name, defaultValue)
            buildConfigField("String", name, "\"$value\"")
        }

        stringField("REVENUECAT_API_KEY")
        stringField("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        stringField("ADMOB_BANNER_AD_UNIT_ID", "ca-app-pub-3940256099942544/9214589741")
        stringField("ADMOB_INTERSTITIAL_AD_UNIT_ID", "ca-app-pub-3940256099942544/1033173712")
        stringField("ADMOB_APP_OPEN_AD_UNIT_ID", "ca-app-pub-3940256099942544/9257395921")
        stringField("ADMOB_NATIVE_AD_UNIT_ID", "ca-app-pub-3940256099942544/2247696110")
        stringField("ADMOB_NATIVE_VIDEO_AD_UNIT_ID", "ca-app-pub-3940256099942544/1044960115")
        stringField("ADMOB_REWARDED_AD_UNIT_ID", "ca-app-pub-3940256099942544/5224354917")
        stringField("ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID", "ca-app-pub-3940256099942544/5354046379")

        missingDimensionStrategy("apis", "defaults")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":purchases"))
    implementation(project(":feature:admob-next-gen"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.google.mobile.ads.next.gen)
}
