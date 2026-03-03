package com.revenuecat.purchases.codegen.generator

import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.api.EntitlementSchema
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CustomerInfoExtGeneratorTest {

    private lateinit var outputDir: File

    @BeforeTest
    fun setUp() {
        outputDir = createTempDirectory().toFile()
    }

    @AfterTest
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    @Test
    fun `generates CustomerInfoExt file`() {
        generate(listOf(ent("premium_access", "Premium")))
        assertTrue(extFile().exists())
    }

    @Test
    fun `CustomerInfoExt has Boolean extension property for active check`() {
        generate(listOf(ent("premium_access", "Premium")))
        val content = extFile().readText()
        assertContains(content, "isPremiumAccessActive")
        assertContains(content, "Boolean")
    }

    @Test
    fun `CustomerInfoExt getter body checks entitlements map by lookup key`() {
        generate(listOf(ent("premium_access", "Premium")))
        val content = extFile().readText()
        assertContains(content, """"premium_access"""")
        assertContains(content, "isActive == true")
        assertContains(content, "entitlements")
    }

    @Test
    fun `CustomerInfoExt receiver type is CustomerInfo`() {
        generate(listOf(ent("premium_access", "Premium")))
        val content = extFile().readText()
        assertContains(content, "CustomerInfo")
    }

    @Test
    fun `multiple entitlements produce multiple extension properties`() {
        generate(
            listOf(
                ent("premium_access", "Premium"),
                ent("basic_access", "Basic"),
                ent("trial_access", "Trial")
            )
        )
        val content = extFile().readText()
        assertContains(content, "isPremiumAccessActive")
        assertContains(content, "isBasicAccessActive")
        assertContains(content, "isTrialAccessActive")
    }

    @Test
    fun `SNAKE_CASE style produces snake_case property name`() {
        generate(
            listOf(ent("premiumAccess", "Premium")),
            style = NamingStyle.SNAKE_CASE
        )
        val content = extFile().readText()
        // CustomerInfoExtGenerator only creates activePropName (no separate propName property)
        assertContains(content, "isPremium_accessActive")
    }

    @Test
    fun `AS_IS style preserves original identifier casing`() {
        generate(
            listOf(ent("PremiumAccess", "Premium")),
            style = NamingStyle.AS_IS
        )
        val content = extFile().readText()
        assertContains(content, "PremiumAccess")
        assertContains(content, "isPremiumAccessActive")
    }

    @Test
    fun `rc prefix stripped from property name`() {
        generate(listOf(ent("\$rc_monthly", "Monthly")))
        val content = extFile().readText()
        assertContains(content, "isMonthlyActive")
        // KotlinPoet escapes $ as ${'$'} in generated string literals; check for rc_monthly
        assertContains(content, "rc_monthly")
    }

    @Test
    fun `reserved keyword entitlement name produces valid active property`() {
        generate(listOf(ent("class", "Class Entitlement")))
        val content = extFile().readText()
        assertContains(content, "isClassActive")
    }

    // --- helpers ---

    private fun generate(
        entitlements: List<EntitlementSchema>,
        style: NamingStyle = NamingStyle.CAMEL_CASE
    ) {
        CustomerInfoExtGenerator("com.example.rc", style).generate(entitlements, outputDir)
    }

    private fun extFile() = File(outputDir, "com/example/rc/CustomerInfoExt.kt")

    private fun ent(lookupKey: String, displayName: String) =
        EntitlementSchema(id = "ent_${lookupKey.hashCode()}", lookupKey = lookupKey, displayName = displayName)
}
