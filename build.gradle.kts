buildscript {
    val kotlinVersion by extra("1.3.72")
    val compileVersion by extra(28)
    val minVersion by extra(14)
    repositories {
        jcenter()
        google()
        maven { url = uri("http://oss.sonatype.org/content/repositories/snapshots/") }
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.9.0")
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.10.0")
        classpath("org.jacoco:org.jacoco.core:0.8.5")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt").version("1.7.2")
    id("com.github.kt3k.coveralls").version("2.10.0")
    id("com.savvasdalkitsis.module-dependency-graph").version("0.9")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.8.0")
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Runs over whole code base without the starting overhead for each module."
    buildUponDefaultConfig = true
    autoCorrect = true
    parallel = true
    setSource(files(rootDir))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/build/**")
    exclude("**/test/**/*.kt")
    baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
    reports {
        xml.enabled = true
        xml.destination = file("build/reports/detekt/detekt.xml")
        html.enabled = false
        txt.enabled = false
    }
}

val detektAllBaseline by tasks.registering(io.gitlab.arturbosch.detekt.DetektCreateBaselineTask::class) {
    description = "Overrides current top level baseline with issues found on this run." +
            "Issues found on the baseline will be ignored on detekt runs."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/build/**")
    exclude("**/test/**/*.kt")
}
