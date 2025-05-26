import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly(libs.dokka.core)
    implementation(libs.dokka.base)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.dokka.testApi)
    testImplementation(libs.dokka.baseTestUtils)
}
