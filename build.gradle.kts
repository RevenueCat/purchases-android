buildscript {
    val kotlin_version by extra("1.3.71")
    repositories {
        jcenter()
        google()
        maven { url = uri("http://oss.sonatype.org/content/repositories/snapshots/") }
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.9.0")
        classpath("com.android.tools.build:gradle:3.6.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.10.0")
        classpath("org.jacoco:org.jacoco.core:0.8.5")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt").version("1.7.2")
    id("com.github.kt3k.coveralls").version("2.10.0")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.8.0")
}

subprojects {
    repositories {
        jcenter()
        google()
    }
}

allprojects {
    repositories {
        jcenter()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Runs over whole code base without the starting overhead for each module."
    autoCorrect = true
    parallel = true
    setSource(files(projectDir))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/build/**")
    exclude("**/test/**/*.kt")
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
    }
}

val detektAllBaseline by tasks.registering(io.gitlab.arturbosch.detekt.DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/build/**")
    exclude("**/test/**/*.kt")
}
