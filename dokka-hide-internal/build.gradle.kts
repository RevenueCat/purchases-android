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

dependencies {
    compileOnly(libs.dokka.core)
    implementation(libs.dokka.base)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.dokka.testApi)
    testImplementation(libs.dokka.baseTestUtils)
    testRuntimeOnly(libs.dokka.analysisKotlinSymbols)
    // Dokka 2.2 K2 analysis needs Kotlin stdlib APIs newer than the repo's 2.0.21 (test runtime only).
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.dokkaAnalysisKotlin.get()}")
}
