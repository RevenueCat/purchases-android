package com.revenuecat.purchases.codegen

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Gradle TestKit functional tests for RevenueCatCodeGenPlugin.
 *
 * Strategy: pre-write a valid cache JSON file so FetchSchemaTask exits early
 * (cache is valid) without making any HTTP requests. This lets us test the
 * full rcGenerateCode happy path without a live RevenueCat API key.
 *
 * GradleTestKit is automatically on the test classpath because the codegen
 * module applies `java-gradle-plugin`.
 */
class RevenueCatCodeGenPluginFunctionalTest {

    private lateinit var projectDir: File

    @BeforeTest
    fun setUp() {
        projectDir = createTempDirectory().toFile()

        // Minimal settings file
        File(projectDir, "settings.gradle.kts").writeText(
            """rootProject.name = "functional-test-project""""
        )

        // Minimal build file — only the revenucat codegen plugin (no Android / Kotlin JVM)
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.revenuecat.purchases.codegen")
            }

            revenuecat {
                apiKey = "sk_test_dummy"
                projectId = "proj_test"
                packageName = "com.example.rc"
            }
            """.trimIndent()
        )
    }

    @AfterTest
    fun tearDown() {
        projectDir.deleteRecursively()
    }

    @Test
    fun `rcGenerateCode succeeds and creates entitlement files from cache`() {
        writeCache(
            entitlements = listOf(
                CacheEntitlement("ent_1", "premium_access", "Premium Access")
            ),
            offerings = emptyList()
        )

        val result = buildProject("rcGenerateCode")

        assertEquals(TaskOutcome.SUCCESS, result.task(":rcGenerateCode")?.outcome)

        val generatedDir = File(projectDir, "build/generated/revenuecat/kotlin/com/example/rc")
        assertTrue(File(generatedDir, "RCEntitlementId.kt").exists())
        assertTrue(File(generatedDir, "EntitlementInfosExt.kt").exists())
        assertTrue(File(generatedDir, "CustomerInfoExt.kt").exists())
    }

    @Test
    fun `rcGenerateCode creates correct entitlement constant value`() {
        writeCache(
            entitlements = listOf(CacheEntitlement("ent_1", "premium_access", "Premium Access")),
            offerings = emptyList()
        )

        buildProject("rcGenerateCode")

        val idFile = File(projectDir, "build/generated/revenuecat/kotlin/com/example/rc/RCEntitlementId.kt")
        val content = idFile.readText()
        assertTrue(content.contains("PREMIUM_ACCESS"))
        assertTrue(content.contains(""""premium_access""""))
    }

    @Test
    fun `rcGenerateCode creates offering and package files from cache`() {
        writeCache(
            entitlements = emptyList(),
            offerings = listOf(
                CacheOffering(
                    id = "off_1",
                    lookupKey = "default",
                    displayName = "Default",
                    isCurrent = true,
                    packages = listOf(CachePackage("pkg_1", "\$rc_monthly", "Monthly"))
                )
            )
        )

        val result = buildProject("rcGenerateCode")

        assertEquals(TaskOutcome.SUCCESS, result.task(":rcGenerateCode")?.outcome)

        val generatedDir = File(projectDir, "build/generated/revenuecat/kotlin/com/example/rc")
        assertTrue(File(generatedDir, "RCOfferingId.kt").exists())
        assertTrue(File(generatedDir, "OfferingsExt.kt").exists())
        assertTrue(File(generatedDir, "RCDefaultPackageId.kt").exists())
    }

    @Test
    fun `rcGenerateCode succeeds with empty schema and creates no generated files`() {
        writeCache(entitlements = emptyList(), offerings = emptyList())

        val result = buildProject("rcGenerateCode")

        assertEquals(TaskOutcome.SUCCESS, result.task(":rcGenerateCode")?.outcome)

        // No entitlement/offering files should be created for empty schema
        val generatedDir = File(projectDir, "build/generated/revenuecat/kotlin/com/example/rc")
        assertTrue(!File(generatedDir, "RCEntitlementId.kt").exists())
        assertTrue(!File(generatedDir, "RCOfferingId.kt").exists())
    }

    @Test
    fun `rcGenerateCode skips gracefully when no cache file exists`() {
        // Do NOT write any cache — the task should warn and return SUCCESS
        // We still need the cacheDir to exist because GenerateCodeTask has @InputDirectory
        val cacheDir = File(projectDir, "build/revenuecat/cache")
        cacheDir.mkdirs()

        // Use -x to skip rcFetchSchema so we don't hit the missing apiKey validation
        // (FetchSchemaTask validates apiKey before checking cache)
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("rcGenerateCode", "-x", "rcFetchSchema", "--stacktrace")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":rcGenerateCode")?.outcome)

        // No generated files since there was no schema
        val generatedDir = File(projectDir, "build/generated/revenuecat/kotlin")
        assertTrue(!File(generatedDir, "com/example/rc/RCEntitlementId.kt").exists())
    }

    // --- helpers ---

    private fun buildProject(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()
        .build()

    private fun writeCache(
        entitlements: List<CacheEntitlement>,
        offerings: List<CacheOffering>
    ) {
        val cacheDir = File(projectDir, "build/revenuecat/cache")
        cacheDir.mkdirs()

        val timestamp = System.currentTimeMillis()

        val entitlementsJson = entitlements.joinToString(",\n") { e ->
            """{"id": "${e.id}", "lookupKey": "${e.lookupKey}", "displayName": "${e.displayName}"}"""
        }

        val offeringsJson = offerings.joinToString(",\n") { o ->
            val packagesJson = o.packages.joinToString(",\n") { p ->
                """{"id": "${p.id}", "lookupKey": "${p.lookupKey}", "displayName": "${p.displayName}"}"""
            }
            """
            {
              "id": "${o.id}",
              "lookupKey": "${o.lookupKey}",
              "displayName": "${o.displayName}",
              "isCurrent": ${o.isCurrent},
              "packages": [$packagesJson]
            }
            """.trimIndent()
        }

        val json = """
            {
              "timestamp": $timestamp,
              "data": {
                "entitlements": [$entitlementsJson],
                "offerings": [$offeringsJson]
              }
            }
        """.trimIndent()

        File(cacheDir, "revenuecat-schema.json").writeText(json)
    }

    private data class CacheEntitlement(val id: String, val lookupKey: String, val displayName: String)
    private data class CacheOffering(
        val id: String,
        val lookupKey: String,
        val displayName: String,
        val isCurrent: Boolean,
        val packages: List<CachePackage>
    )
    private data class CachePackage(val id: String, val lookupKey: String, val displayName: String)
}
