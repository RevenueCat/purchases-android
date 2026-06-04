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

class EntitlementsGeneratorTest {

    private lateinit var outputDir: File

    @BeforeTest
    fun setUp() {
        outputDir = createTempDirectory().toFile()
    }

    @AfterTest
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    // --- RCEntitlementId object ---

    @Test
    fun `generates RCEntitlementId file`() {
        generate(listOf(ent("premium_access", "Premium Access")))
        assertTrue(idFile().exists())
    }

    @Test
    fun `RCEntitlementId contains const val for each entitlement`() {
        generate(
            listOf(
                ent("premium_access", "Premium"),
                ent("basic", "Basic")
            )
        )
        val content = idFile().readText()
        assertContains(content, """PREMIUM_ACCESS""")
        assertContains(content, """"premium_access"""")
        assertContains(content, """BASIC""")
        assertContains(content, """"basic"""")
    }

    @Test
    fun `RCEntitlementId strips rc prefix for constant name`() {
        generate(listOf(ent("\$rc_monthly", "Monthly")))
        val content = idFile().readText()
        assertContains(content, "MONTHLY")
        // The value should contain the original lookup key (KotlinPoet escapes $ as ${'$'})
        assertContains(content, "rc_monthly")
    }

    // --- EntitlementInfosExt extensions ---

    @Test
    fun `generates EntitlementInfosExt file`() {
        generate(listOf(ent("premium_access", "Premium")))
        assertTrue(extFile().exists())
    }

    @Test
    fun `EntitlementInfosExt has nullable accessor property`() {
        generate(listOf(ent("premium_access", "Premium")))
        val content = extFile().readText()
        assertContains(content, "premiumAccess")
        assertContains(content, "EntitlementInfo?")
        assertContains(content, """"premium_access"""")
    }

    @Test
    fun `EntitlementInfosExt has isActive boolean extension`() {
        generate(listOf(ent("premium_access", "Premium")))
        val content = extFile().readText()
        assertContains(content, "isPremiumAccessActive")
        assertContains(content, "Boolean")
        assertContains(content, "isActive == true")
    }

    @Test
    fun `SNAKE_CASE style produces snake_case accessor name`() {
        generate(
            listOf(ent("premiumAccess", "Premium")),
            style = NamingStyle.SNAKE_CASE
        )
        val content = extFile().readText()
        assertContains(content, "premium_access")
        assertContains(content, "isPremium_accessActive")
    }

    @Test
    fun `AS_IS style preserves original identifier`() {
        generate(
            listOf(ent("PremiumAccess", "Premium")),
            style = NamingStyle.AS_IS
        )
        val content = extFile().readText()
        assertContains(content, "PremiumAccess")
    }

    @Test
    fun `reserved keyword entitlement name is backtick-wrapped`() {
        generate(listOf(ent("class", "Class Entitlement")))
        val content = extFile().readText()
        assertContains(content, "`class`")
    }

    @Test
    fun `multiple entitlements all appear in extension file`() {
        generate(
            listOf(
                ent("premium_access", "Premium"),
                ent("basic_access", "Basic"),
                ent("trial_access", "Trial")
            )
        )
        val content = extFile().readText()
        assertContains(content, "premiumAccess")
        assertContains(content, "basicAccess")
        assertContains(content, "trialAccess")
    }

    @Test
    fun `empty entitlements list still creates files`() {
        generate(emptyList())
        // Files are still created, just with empty objects/no properties
        assertTrue(idFile().exists())
        assertTrue(extFile().exists())
    }

    // --- helpers ---

    private fun generate(
        entitlements: List<EntitlementSchema>,
        style: NamingStyle = NamingStyle.CAMEL_CASE
    ) {
        EntitlementsGenerator("com.example.rc", style).generate(entitlements, outputDir)
    }

    private fun idFile() = File(outputDir, "com/example/rc/RCEntitlementId.kt")
    private fun extFile() = File(outputDir, "com/example/rc/EntitlementInfosExt.kt")

    private fun ent(lookupKey: String, displayName: String) =
        EntitlementSchema(id = "ent_${lookupKey.hashCode()}", lookupKey = lookupKey, displayName = displayName)
}
