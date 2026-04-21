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
}
