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
    implementation(libs.kotlinpoet)
    implementation(libs.json)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.okhttp.mockwebserver)
}

gradlePlugin {
    plugins {
        create("revenuecatCodegen") {
            id = "com.revenuecat.purchases.codegen"
            implementationClass = "com.revenuecat.purchases.codegen.RevenueCatCodeGenPlugin"
        }
    }
}
