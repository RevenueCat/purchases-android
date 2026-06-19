package com.revenuecat.purchases.codegen.cache

import com.revenuecat.purchases.codegen.api.EntitlementSchema
import com.revenuecat.purchases.codegen.api.OfferingSchema
import com.revenuecat.purchases.codegen.api.PackageSchema
import com.revenuecat.purchases.codegen.api.ProjectSchema
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaCacheTest {

    private lateinit var tempDir: File
    private lateinit var cache: SchemaCache

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory().toFile()
        cache = SchemaCache(tempDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // --- isValid ---

    @Test
    fun `isValid returns false when no cache file exists`() {
        assertFalse(cache.isValid(60))
    }

    @Test
    fun `isValid returns true for freshly written cache within TTL`() {
        cache.write(emptySchema())
        assertTrue(cache.isValid(60))
    }

    @Test
    fun `isValid returns false for expired cache`() {
        val twoHoursAgo = System.currentTimeMillis() - 120 * 60_000L
        File(tempDir, "revenuecat-schema.json").writeText(
            """{"timestamp": $twoHoursAgo, "data": {"entitlements": [], "offerings": []}}"""
        )
        assertFalse(cache.isValid(60))
    }

    @Test
    fun `isValid returns false for corrupted JSON`() {
        File(tempDir, "revenuecat-schema.json").writeText("not valid json {{{")
        assertFalse(cache.isValid(60))
    }

    @Test
    fun `isValid returns false when timestamp field is missing`() {
        File(tempDir, "revenuecat-schema.json").writeText("""{"data": {}}""")
        assertFalse(cache.isValid(60))
    }

    // --- cacheAgeMinutes ---

    @Test
    fun `cacheAgeMinutes returns null when no file`() {
        assertNull(cache.cacheAgeMinutes())
    }

    @Test
    fun `cacheAgeMinutes returns near zero for freshly written cache`() {
        cache.write(emptySchema())
        val age = cache.cacheAgeMinutes()
        assertNotNull(age)
        assertTrue(age <= 1)
    }

    @Test
    fun `cacheAgeMinutes returns null for corrupted file`() {
        File(tempDir, "revenuecat-schema.json").writeText("{invalid}")
        assertNull(cache.cacheAgeMinutes())
    }

    @Test
    fun `cacheAgeMinutes reflects expected age for old timestamp`() {
        val oneHourAgo = System.currentTimeMillis() - 60 * 60_000L
        File(tempDir, "revenuecat-schema.json").writeText(
            """{"timestamp": $oneHourAgo, "data": {"entitlements": [], "offerings": []}}"""
        )
        val age = cache.cacheAgeMinutes()
        assertNotNull(age)
        assertTrue(age in 59..61)
    }

    // --- read ---

    @Test
    fun `read returns null when no file`() {
        assertNull(cache.read())
    }

    @Test
    fun `read returns null for corrupted file`() {
        File(tempDir, "revenuecat-schema.json").writeText("{corrupt}")
        assertNull(cache.read())
    }

    @Test
    fun `read round-trips entitlements`() {
        val schema = ProjectSchema(
            entitlements = listOf(
                EntitlementSchema("ent_1", "premium_access", "Premium Access"),
                EntitlementSchema("ent_2", "\$rc_pro", "Pro Plan")
            ),
            offerings = emptyList()
        )
        cache.write(schema)
        val result = cache.read()

        assertNotNull(result)
        assertEquals(2, result.entitlements.size)
        assertEquals("ent_1", result.entitlements[0].id)
        assertEquals("premium_access", result.entitlements[0].lookupKey)
        assertEquals("Premium Access", result.entitlements[0].displayName)
        assertEquals("\$rc_pro", result.entitlements[1].lookupKey)
        assertEquals("Pro Plan", result.entitlements[1].displayName)
    }

    @Test
    fun `read round-trips offerings with packages`() {
        val schema = ProjectSchema(
            entitlements = emptyList(),
            offerings = listOf(
                OfferingSchema(
                    id = "off_1",
                    lookupKey = "default",
                    displayName = "Default",
                    isCurrent = true,
                    packages = listOf(
                        PackageSchema("pkg_1", "\$rc_monthly", "Monthly"),
                        PackageSchema("pkg_2", "\$rc_annual", "Annual")
                    )
                )
            )
        )
        cache.write(schema)
        val result = cache.read()

        assertNotNull(result)
        assertEquals(1, result.offerings.size)
        val offering = result.offerings[0]
        assertEquals("off_1", offering.id)
        assertEquals("default", offering.lookupKey)
        assertEquals("Default", offering.displayName)
        assertTrue(offering.isCurrent)
        assertEquals(2, offering.packages.size)
        assertEquals("\$rc_monthly", offering.packages[0].lookupKey)
        assertEquals("Monthly", offering.packages[0].displayName)
        assertEquals("\$rc_annual", offering.packages[1].lookupKey)
    }

    // --- write ---

    @Test
    fun `write creates missing parent directories`() {
        val nested = File(tempDir, "a/b/c")
        val nestedCache = SchemaCache(nested)
        nestedCache.write(emptySchema())
        assertTrue(File(nested, "revenuecat-schema.json").exists())
    }

    @Test
    fun `write produces JSON with timestamp field`() {
        cache.write(emptySchema())
        val text = File(tempDir, "revenuecat-schema.json").readText()
        assertTrue(text.contains("\"timestamp\""))
    }

    @Test
    fun `written file is re-readable after write`() {
        val schema = ProjectSchema(
            entitlements = listOf(EntitlementSchema("e1", "basic", "Basic")),
            offerings = emptyList()
        )
        cache.write(schema)
        val result = cache.read()
        assertNotNull(result)
        assertEquals("basic", result.entitlements[0].lookupKey)
    }

    // --- helpers ---

    private fun emptySchema() = ProjectSchema(emptyList(), emptyList())
}
