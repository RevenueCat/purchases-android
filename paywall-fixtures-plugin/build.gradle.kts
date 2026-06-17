plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

if (!project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString().contains("customEntitlementComputation")) {
    apply(plugin = "com.vanniktech.maven.publish")
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    implementation(gradleApi())
    implementation(libs.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.okhttp.mockwebserver)
}

gradlePlugin {
    plugins {
        create("revenuecatPaywallFixtures") {
            id = "com.revenuecat.purchases.paywallfixtures"
            implementationClass = "com.revenuecat.purchases.paywallfixtures.PaywallFixturesPlugin"
        }
    }
}
