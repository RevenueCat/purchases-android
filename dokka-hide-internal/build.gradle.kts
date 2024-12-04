plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.dokka.core)
    implementation(libs.dokka.base)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.dokka.testApi)
    testImplementation(libs.dokka.baseTestUtils)
}
