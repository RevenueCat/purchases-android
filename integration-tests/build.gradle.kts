plugins {
    id("revenuecat-android-application")
}

fun obtainTestBuildType(): String {
    var result = "debug"

    if (project.hasProperty("testBuildType")) {
        result = project.property("testBuildType").toString()
    }

    return result
}

android {
    defaultConfig {
        applicationId = "com.revenuecat.purchases.integrationtests"
        minSdk = 21
        versionCode = 1
        versionName = "1.0"

        // Library modules have a dimension used to separate different apis.
        // Our applications however don't need the extra flavor. This makes sure that we use the
        // default flavor.
        missingDimensionStrategy("apis", "defaults")
        missingDimensionStrategy("billingclient", "bc8")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    signingConfigs {
        create("release") {
            storeFile = file("keystore")
            storePassword = project.properties["releaseKeystorePassword"]?.toString()
            keyAlias = project.properties["releaseKeyAlias"]?.toString()
            keyPassword = project.properties["releaseKeyPassword"]?.toString()
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            testProguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    testBuildType = obtainTestBuildType()
    namespace = "com.revenuecat.purchases.integrationtests"
}

dependencies {
    implementation(project(":purchases"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.multidex)

    androidTestImplementation(project(":purchases"))
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit)
}
