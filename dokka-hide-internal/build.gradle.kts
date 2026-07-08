import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = "17"
}

// Dokka 2.2 K2 analysis needs Kotlin stdlib APIs newer than the repo's 2.0.21 (test runtime only).
private val dokkaTestKotlinVersion = "2.2.0"

configurations.testRuntimeClasspath {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:$dokkaTestKotlinVersion")
}

dependencies {
    compileOnly(libs.dokka.core)
    implementation(libs.dokka.base)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.dokka.testApi)
    testImplementation(libs.dokka.baseTestUtils)
    testRuntimeOnly(libs.dokka.analysisKotlinSymbols)
}
