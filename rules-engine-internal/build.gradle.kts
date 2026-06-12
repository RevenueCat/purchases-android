plugins {
    alias(libs.plugins.revenuecat.public.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.revenuecat.purchases.rules"
}

dependencies {
    // `org.json` is provided by the Android platform for the main source set;
    // this dependency adds a real implementation to the JVM unit-test classpath
    // (which exercises the production `ValueJson` parser).
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.test)
}
