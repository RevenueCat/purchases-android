package com.revenuecat.purchases.uifixtures

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Gradle TestKit functional tests for UiFixturesPlugin. The end-to-end recording flow is covered by
 * FixtureRecorderTest; these tests cover plugin wiring and configuration errors.
 */
class UiFixturesPluginFunctionalTest {

    private lateinit var projectDir: File

    @BeforeTest
    fun setUp() {
        projectDir = createTempDirectory().toFile()
        File(projectDir, "settings.gradle.kts").writeText("""rootProject.name = "functional-test-project"""")
    }

    @AfterTest
    fun tearDown() {
        projectDir.deleteRecursively()
    }

    @Test
    fun `recordPaywallFixtures fails with a descriptive error when no api key is configured`() {
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.revenuecat.purchases.uifixtures")
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("recordPaywallFixtures")
            .withPluginClasspath()
            .withEnvironment(emptyMap())
            .buildAndFail()

        assertTrue("REVENUECAT_API_KEY" in result.output)
        assertTrue("uiFixtures" in result.output)
    }

    @Test
    fun `task is registered with group and description`() {
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.revenuecat.purchases.uifixtures")
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("tasks", "--group", "revenuecat")
            .withPluginClasspath()
            .build()

        assertTrue("recordPaywallFixtures" in result.output)
    }
}
