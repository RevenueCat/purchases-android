import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.revenuecat.purchases.android.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("AndroidLibrary") {
            id = "revenuecat-android-library"
            implementationClass =
                "com.revenuecat.purchases.android.buildlogic.plugin.AndroidLibraryConventionPlugin"
        }
        register("PublicLibrary") {
            id = "revenuecat-public-library"
            implementationClass =
                "com.revenuecat.purchases.android.buildlogic.plugin.PublicLibraryConventionPlugin"
        }
        register("AndroidApplication") {
            id = "revenuecat-android-application"
            implementationClass =
                "com.revenuecat.purchases.android.buildlogic.plugin.AndroidApplicationConventionPlugin"
        }
        register("ApiTesterApplication") {
            id = "revenuecat-api-tester-application"
            implementationClass =
                "com.revenuecat.purchases.android.buildlogic.plugin.ApiTesterApplicationConventionPlugin"
        }
    }
}
