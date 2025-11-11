plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

if (!(project.properties["ANDROID_VARIANT_TO_PUBLISH"] as String).contains("customEntitlementComputation")) {
    apply(plugin = "com.vanniktech.maven.publish")
}

apply(from = "${rootProject.projectDir}/library.gradle")

android {
    namespace = "com.revenuecat.purchases.amazon"

    flavorDimensions += "apis"
    flavorDimensions += "billingclient"

    productFlavors {
        create("defaults") {
            dimension = "apis"
            isDefault = true
        }
        create("bc8") {
            dimension = "billingclient"
            isDefault = true
        }
    }

    defaultConfig {
        missingDimensionStrategy("apis", "defaults")
    }
}

dependencies {
    implementation(project(":purchases"))

    implementation(libs.amazon.appstore.sdk)
}
