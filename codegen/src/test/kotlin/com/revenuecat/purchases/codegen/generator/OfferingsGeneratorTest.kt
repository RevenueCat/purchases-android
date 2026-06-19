package com.revenuecat.purchases.codegen.generator

import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.api.OfferingSchema
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class OfferingsGeneratorTest {

    private lateinit var outputDir: File

    @BeforeTest
    fun setUp() {
        outputDir = createTempDirectory().toFile()
    }

    @AfterTest
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    // --- RCOfferingId object ---

    @Test
    fun `generates RCOfferingId file`() {
        generate(listOf(offering("default", "Default")))
        assertTrue(idFile().exists())
    }

    @Test
    fun `RCOfferingId contains const val for each offering`() {
        generate(
            listOf(
                offering("default", "Default"),
                offering("premium", "Premium")
            )
        )
        val content = idFile().readText()
        assertContains(content, "DEFAULT")
        assertContains(content, """"default"""")
        assertContains(content, "PREMIUM")
        assertContains(content, """"premium"""")
    }

    @Test
    fun `RCOfferingId const val uses toConstant conversion`() {
        generate(listOf(offering("premium_plan", "Premium Plan")))
        val content = idFile().readText()
        assertContains(content, "PREMIUM_PLAN")
        assertContains(content, """"premium_plan"""")
    }

    // --- OfferingsExt extensions ---

    @Test
    fun `generates OfferingsExt file`() {
        generate(listOf(offering("default", "Default")))
        assertTrue(extFile().exists())
    }

    @Test
    fun `OfferingsExt has nullable Offering extension property`() {
        generate(listOf(offering("default", "Default")))
        val content = extFile().readText()
        assertContains(content, "Offering?")
        assertContains(content, "getOffering")
        assertContains(content, """"default"""")
    }

    @Test
    fun `OfferingsExt property name applies CAMEL_CASE to lookup key`() {
        generate(
            listOf(offering("premium_plan", "Premium Plan")),
            style = NamingStyle.CAMEL_CASE
        )
        val content = extFile().readText()
        assertContains(content, "premiumPlan")
    }

    @Test
    fun `OfferingsExt property name applies SNAKE_CASE to lookup key`() {
        generate(
            listOf(offering("premiumPlan", "Premium Plan")),
            style = NamingStyle.SNAKE_CASE
        )
        val content = extFile().readText()
        assertContains(content, "premium_plan")
    }

    @Test
    fun `OfferingsExt property name uses AS_IS`() {
        generate(
            listOf(offering("MyOffering", "My Offering")),
            style = NamingStyle.AS_IS
        )
        val content = extFile().readText()
        assertContains(content, "MyOffering")
    }

    @Test
    fun `multiple offerings all appear in extension file`() {
        generate(
            listOf(
                offering("default", "Default"),
                offering("premium", "Premium"),
                offering("trial", "Trial")
            )
        )
        val content = extFile().readText()
        assertContains(content, "default")
        assertContains(content, "premium")
        assertContains(content, "trial")
    }

    // --- helpers ---

    private fun generate(
        offerings: List<OfferingSchema>,
        style: NamingStyle = NamingStyle.CAMEL_CASE
    ) {
        OfferingsGenerator("com.example.rc", style).generate(offerings, outputDir)
    }

    private fun idFile() = File(outputDir, "com/example/rc/RCOfferingId.kt")
    private fun extFile() = File(outputDir, "com/example/rc/OfferingsExt.kt")

    private fun offering(lookupKey: String, displayName: String) = OfferingSchema(
        id = "off_${lookupKey.hashCode()}",
        lookupKey = lookupKey,
        displayName = displayName,
        isCurrent = false,
        packages = emptyList()
    )
}
