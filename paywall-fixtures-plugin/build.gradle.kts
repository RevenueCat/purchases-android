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
    // NOTE: deliberately no dependency on the Paparazzi Gradle plugin. The plugin reacts to the
    // consumer's own Paparazzi (if applied) rather than bringing its own, so it never forces a Paparazzi
    // version onto the consumer's buildscript classpath.

    testImplementation(libs.kotlin.test)
    testImplementation(libs.okhttp.mockwebserver)
}

// Bake the SDK version into the plugin jar so the auto-added `purchases-ui-testing` dependency always
// matches the released plugin/SDK version, instead of a hardcoded string that goes stale on version bumps.
val sdkVersion = providers.gradleProperty("VERSION_NAME").orElse(provider { project.version.toString() })
val generateVersionResource = tasks.register("generatePaywallFixturesVersionResource") {
    val outputDir = layout.buildDirectory.dir("generated/paywallFixturesVersion")
    outputs.dir(outputDir)
    val versionValue = sdkVersion
    doLast {
        val file = outputDir.get()
            .file("com/revenuecat/purchases/paywallfixtures/version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=${versionValue.get()}\n")
    }
}
sourceSets.named("main") {
    resources.srcDir(generateVersionResource)
}

gradlePlugin {
    plugins {
        create("revenuecatPaywallFixtures") {
            id = "com.revenuecat.purchases.paywallfixtures"
            implementationClass = "com.revenuecat.purchases.paywallfixtures.PaywallFixturesPlugin"
        }
    }
}
