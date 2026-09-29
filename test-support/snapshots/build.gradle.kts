plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.paparazzi.runtime)
    api(libs.kotlinx.serialization.json)
}
