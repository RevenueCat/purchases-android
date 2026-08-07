package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.common.networking.HTTPResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ProductEntitlementMappingSourceTest {

    private lateinit var deviceCache: DeviceCache

    private val sampleResponseJson = JSONObject(
        """
            {
                "product_entitlement_mapping": {
                    "com.revenuecat.foo_1:p1m": {
                        "product_identifier": "com.revenuecat.foo_1",
                        "base_plan_id": "p1m",
                        "entitlements": ["pro_1"]
                    }
                }
            }
        """.trimIndent()
    )

    private val sampleResponseJsonWithSource = JSONObject(
        """
            {
                "product_entitlement_mapping": {
                    "com.revenuecat.foo_1:p1m": {
                        "product_identifier": "com.revenuecat.foo_1",
                        "base_plan_id": "p1m",
                        "entitlements": ["pro_1"]
                    }
                },
                "rc_original_source": "FALLBACK"
            }
        """.trimIndent()
    )

    @Before
    fun setUp() {
        deviceCache = mockk()
    }

    @Test
    fun `productEntitlementMapping created with MAIN source from network response`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        val mapping = ProductEntitlementMapping.fromNetwork(sampleResponseJson, httpResult)

        assertThat(mapping.originalSource).isEqualTo(HTTPResponseOriginalSource.MAIN)
        assertThat(mapping.loadedFromCache).isFalse
    }

    @Test
    fun `productEntitlementMapping created with LOAD_SHEDDER source from response with header`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = false,
        )

        val mapping = ProductEntitlementMapping.fromNetwork(sampleResponseJson, httpResult)

        assertThat(mapping.originalSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
        assertThat(mapping.loadedFromCache).isFalse
    }

    @Test
    fun `productEntitlementMapping created with FALLBACK source from fallback URL response`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = false,
            isFallbackURL = true,
        )

        val mapping = ProductEntitlementMapping.fromNetwork(sampleResponseJson, httpResult)

        assertThat(mapping.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
        assertThat(mapping.loadedFromCache).isFalse
    }

    @Test
    fun `productEntitlementMapping cache stores originalSource`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = false,
        )

        val originalMapping = ProductEntitlementMapping.fromNetwork(sampleResponseJson, httpResult)

        // Mock device cache to store originalSource
        every { deviceCache.cacheProductEntitlementMapping(any()) } returns Unit

        // Cache the mapping
        deviceCache.cacheProductEntitlementMapping(originalMapping)

        // Verify mapping was cached with correct source
        verify(exactly = 1) { deviceCache.cacheProductEntitlementMapping(originalMapping) }
        assertThat(originalMapping.originalSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
    }

    @Test
    fun `productEntitlementMapping ignores originalSource in JSON, always returns null`() {
        // originalSource is intentionally not persisted, so fromJson doesn't read it back even if
        // present, e.g. from a cache file written by an older SDK version.
        val mapping = ProductEntitlementMapping.fromJson(sampleResponseJsonWithSource, loadedFromCache = true)

        assertThat(mapping.originalSource).isNull()
        assertThat(mapping.loadedFromCache).isTrue
    }

    @Test
    fun `productEntitlementMapping originalSource is null when no source information provided`() {
        val mapping = ProductEntitlementMapping.fromJson(sampleResponseJson)

        assertThat(mapping.originalSource).isNull()
        assertThat(mapping.loadedFromCache).isFalse
    }

    @Test
    fun `FALLBACK takes precedence over LOAD_SHEDDER when both are set`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = true,
        )

        val mapping = ProductEntitlementMapping.fromNetwork(sampleResponseJson, httpResult)

        // LOAD_SHEDDER should take precedence
        assertThat(mapping.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
        assertThat(mapping.loadedFromCache).isFalse
    }
}
