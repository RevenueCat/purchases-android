import java.io.File
import java.net.URL

plugins {
    id("revenuecat-public-library")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.poko)
}

android {
    namespace = "com.revenuecat.purchases.galaxy"

    // billingclient dimension is added for bc7/bc8 support
    flavorDimensions += "billingclient"

    productFlavors {
        create("bc8") {
            dimension = "billingclient"
            isDefault = true
        }
        create("bc7") {
            dimension = "billingclient"
        }
    }

    defaultConfig {
        missingDimensionStrategy("apis", "defaults")
    }
}

val samsungIapVersion = libs.versions.samsungIap.get()
val samsungIapFileName = "samsung-iap-$samsungIapVersion.aar"
val samsungIapDestFile = rootProject.file("libs/$samsungIapFileName")

val isCi = System.getenv("CI") == "true"

tasks.register("getSamsungIapSdk") {
    val downloadUrl = System.getenv("SAMSUNG_IAP_SDK_URL").orEmpty()

    inputs.property("downloadURL", downloadUrl)
    inputs.property("fileToExtract", samsungIapFileName)
    outputs.file(samsungIapDestFile)

    doLast {
        if (samsungIapDestFile.exists()) {
            return@doLast
        }
        if (downloadUrl.isBlank()) {
            throw GradleException("SAMSUNG_IAP_SDK_URL is not set")
        }

        logger.lifecycle("Downloading Samsung IAP SDK")
        samsungIapDestFile.parentFile.mkdirs()

        val downloadFile = File(temporaryDir, "download.zip")
        URL(downloadUrl).openStream().use { input ->
            downloadFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (downloadUrl.lowercase().endsWith(".zip")) {
            project.copy {
                from(
                    zipTree(downloadFile)
                        .matching { include("**/$samsungIapFileName") }
                        .singleFile,
                )
                into(samsungIapDestFile.parentFile)
            }
        } else {
            downloadFile.copyTo(samsungIapDestFile, overwrite = true)
        }
    }
}

tasks.register<Delete>("cleanSamsungIapSdk") {
    delete(samsungIapDestFile)
}

if (isCi) {
    tasks.named("preBuild") {
        dependsOn("getSamsungIapSdk")
    }
}

dependencies {
    implementation(project(":purchases"))

    compileOnly("com.samsung.iap:samsung-iap:$samsungIapVersion@aar")
    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlin.test)
    testImplementation("com.samsung.iap:samsung-iap:$samsungIapVersion@aar")
}
