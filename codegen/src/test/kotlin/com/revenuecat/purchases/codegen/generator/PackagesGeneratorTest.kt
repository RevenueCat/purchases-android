package com.revenuecat.purchases.codegen.generator

import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.api.OfferingSchema
import com.revenuecat.purchases.codegen.api.PackageSchema
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackagesGeneratorTest {

    private lateinit var outputDir: File

    @BeforeTest
    fun setUp() {
        outputDir = createTempDirectory().toFile()
    }

    @AfterTest
    fun tearDown() {
        outputDir.deleteRecursively()
    }

    // --- per-offering ID object ---

    @Test
    fun `generates per-offering package ID file`() {
        generate(listOf(offeringWithPackages("default", listOf(pkg("\$rc_monthly", "Monthly")))))
        assertTrue(pkgIdFile("RCDefaultPackageId").exists())
    }

    @Test
    fun `per-offering ID object contains const val with original lookup key as value`() {
        generate(listOf(offeringWithPackages("default", listOf(pkg("\$rc_monthly", "Monthly")))))
        val content = pkgIdFile("RCDefaultPackageId").readText()
        assertContains(content, "MONTHLY")
        // KotlinPoet escapes $ as ${'$'} in string literals; verify rc_monthly key is present
        assertContains(content, "rc_monthly")
    }

    @Test
    fun `per-offering ID object contains all packages`() {
        generate(
            listOf(
                offeringWithPackages(
                    "default",
                    listOf(
                        pkg("\$rc_monthly", "Monthly"),
                        pkg("\$rc_annual", "Annual"),
                        pkg("\$rc_weekly", "Weekly")
                    )
                )
            )
        )
        val content = pkgIdFile("RCDefaultPackageId").readText()
        assertContains(content, "MONTHLY")
        assertContains(content, "ANNUAL")
        assertContains(content, "WEEKLY")
    }

    // --- extension properties ---

    @Test
    fun `per-offering file contains extension property with offering prefix`() {
        generate(listOf(offeringWithPackages("default", listOf(pkg("\$rc_monthly", "Monthly")))))
        val content = pkgIdFile("RCDefaultPackageId").readText()
        // Extension property name: offering prefix + package identifier capitalised
        assertContains(content, "defaultMonthly")
        assertContains(content, "Package?")
        assertContains(content, "getPackage")
        assertContains(content, "rc_monthly")
    }

    @Test
    fun `collision prevention produces distinct extension names for shared package keys`() {
        // Two offerings both have "$rc_monthly" — extension names must not clash
        generate(
            listOf(
                offeringWithPackages("default", listOf(pkg("\$rc_monthly", "Monthly"))),
                offeringWithPackages("premium", listOf(pkg("\$rc_monthly", "Monthly")))
            )
        )
        val defaultContent = pkgIdFile("RCDefaultPackageId").readText()
        val premiumContent = pkgIdFile("RCPremiumPackageId").readText()

        assertContains(defaultContent, "defaultMonthly")
        assertContains(premiumContent, "premiumMonthly")
        // Neither file should have the other offering's prefix
        assertFalse(defaultContent.contains("premiumMonthly"))
        assertFalse(premiumContent.contains("defaultMonthly"))
    }

    @Test
    fun `offering with empty packages produces no file`() {
        generate(
            listOf(
                OfferingSchema("off_1", "default", "Default", false, emptyList())
            )
        )
        assertFalse(pkgIdFile("RCDefaultPackageId").exists())
    }

    @Test
    fun `SNAKE_CASE style applies to extension property name`() {
        generate(
            listOf(offeringWithPackages("myOffering", listOf(pkg("monthlyPlan", "Monthly Plan")))),
            style = NamingStyle.SNAKE_CASE
        )
        val content = pkgIdFile("RCMy_offeringPackageId").readText()
        // toIdentifier("myOffering", SNAKE_CASE) = "my_offering"
        // toIdentifier("monthlyPlan", SNAKE_CASE) = "monthly_plan"
        // extension propName = "my_offering" + "Monthly_plan" = "my_offeringMonthly_plan"
        assertContains(content, "my_offeringMonthly_plan")
    }

    @Test
    fun `multiple packages in one offering all get extension properties`() {
        generate(
            listOf(
                offeringWithPackages(
                    "default",
                    listOf(
                        pkg("\$rc_monthly", "Monthly"),
                        pkg("\$rc_annual", "Annual")
                    )
                )
            )
        )
        val content = pkgIdFile("RCDefaultPackageId").readText()
        assertContains(content, "defaultMonthly")
        assertContains(content, "defaultAnnual")
    }

    @Test
    fun `reserved keyword names produce valid composite identifiers`() {
        generate(listOf(offeringWithPackages("in", listOf(pkg("class", "Class")))))
        val content = pkgIdFile("RCInPackageId").readText()
        assertContains(content, "inClass")
    }

    @Test
    fun `generates separate files for multiple offerings`() {
        generate(
            listOf(
                offeringWithPackages("default", listOf(pkg("\$rc_monthly", "Monthly"))),
                offeringWithPackages("premium", listOf(pkg("\$rc_annual", "Annual")))
            )
        )
        assertTrue(pkgIdFile("RCDefaultPackageId").exists())
        assertTrue(pkgIdFile("RCPremiumPackageId").exists())
    }

    // --- helpers ---

    private fun generate(
        offerings: List<OfferingSchema>,
        style: NamingStyle = NamingStyle.CAMEL_CASE
    ) {
        PackagesGenerator("com.example.rc", style).generate(offerings, outputDir)
    }

    private fun pkgIdFile(objectName: String) =
        File(outputDir, "com/example/rc/$objectName.kt")

    private fun offeringWithPackages(lookupKey: String, packages: List<PackageSchema>) =
        OfferingSchema(
            id = "off_${lookupKey.hashCode()}",
            lookupKey = lookupKey,
            displayName = lookupKey.replaceFirstChar { it.uppercase() },
            isCurrent = false,
            packages = packages
        )

    private fun pkg(lookupKey: String, displayName: String) =
        PackageSchema(id = "pkg_${lookupKey.hashCode()}", lookupKey = lookupKey, displayName = displayName)
}
