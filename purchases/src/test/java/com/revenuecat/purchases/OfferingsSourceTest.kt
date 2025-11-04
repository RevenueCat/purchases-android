package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DataSource
import com.revenuecat.purchases.common.OriginalDataSource
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.GoogleOfferingParser
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.common.originalDataSource
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsSourceTest {

    private lateinit var offeringParser: OfferingParser
    private lateinit var deviceCache: DeviceCache
    private lateinit var offeringsCache: OfferingsCache

    @Before
    fun setUp() {
        offeringParser = GoogleOfferingParser()
        deviceCache = mockk()
        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        every { deviceCache.getOfferingsResponseCache() } returns null
        offeringsCache = OfferingsCache(deviceCache, localeProvider = com.revenuecat.purchases.common.DefaultLocaleProvider())
    }

    @Test
    fun `offerings created with MAIN source from network response`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = null,
            isFallbackURL = null,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        assertThat(offerings.originalSource).isEqualTo(OriginalDataSource.MAIN)
        assertThat(offerings.source).isEqualTo(DataSource.MAIN)
    }

    @Test
    fun `offerings created with LOAD_SHEDDER source from response with header`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = null,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        assertThat(offerings.originalSource).isEqualTo(OriginalDataSource.LOAD_SHEDDER)
        assertThat(offerings.source).isEqualTo(DataSource.LOAD_SHEDDER)
    }

    @Test
    fun `offerings created with FALLBACK source from fallback URL response`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = null,
            isFallbackURL = true,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        assertThat(offerings.originalSource).isEqualTo(OriginalDataSource.FALLBACK)
        assertThat(offerings.source).isEqualTo(DataSource.FALLBACK)
    }

    @Test
    fun `offerings created with CACHE source when origin is CACHE`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.CACHE,
            isLoadShedderResponse = null,
            isFallbackURL = null,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        assertThat(offerings.originalSource).isEqualTo(OriginalDataSource.MAIN)
        assertThat(offerings.source).isEqualTo(DataSource.MAIN)
    }

    @Test
    fun `offerings cache stores and restores originalSource`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create offerings with LOAD_SHEDDER source
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = null,
        )
        val originalOfferings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        // Cache the offerings
        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        offeringsCache.cacheOfferings(originalOfferings, offeringsJson)

        // Verify originalSource was stored in JSON
        io.mockk.verify(exactly = 1) {
            deviceCache.cacheOfferingsResponse(any())
        }
    }

    @Test
    fun `offerings cache preserves originalSource when reading from cache`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create and cache offerings with FALLBACK source
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = null,
            isFallbackURL = true,
        )
        val originalOfferings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        // Mock cached response with originalSource in JSON
        val cachedJsonWithSource = JSONObject(ONE_OFFERINGS_RESPONSE).apply {
            put(OfferingsCache.ORIGINAL_SOURCE_KEY, OriginalDataSource.FALLBACK.name)
        }
        every { deviceCache.getOfferingsResponseCache() } returns cachedJsonWithSource
        offeringsCache.cacheOfferings(originalOfferings, offeringsJson)

        // Retrieve from cache - originalSource should be preserved
        val cachedOfferings = offeringsCache.cachedOfferings

        assertThat(cachedOfferings).isNotNull
        assertThat(cachedOfferings!!.originalSource).isEqualTo(OriginalDataSource.FALLBACK)
        assertThat(cachedOfferings.source).isEqualTo(DataSource.FALLBACK)
    }

    @Test
    fun `offerings defaults to MAIN when no source information provided`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create offerings without specifying source (should default to MAIN)
        val offerings = offeringParser.createOfferings(offeringsJson, productsById)

        assertThat(offerings.originalSource).isEqualTo(OriginalDataSource.MAIN)
        assertThat(offerings.source).isEqualTo(DataSource.MAIN)
    }

    @Test
    fun `offerings cache handles missing originalSource gracefully`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create offerings with MAIN source
        val originalOfferings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            OriginalDataSource.MAIN,
            DataSource.MAIN,
        )

        // Cache without storing originalSource (simulating old cache)
        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        // Mock cached response without originalSource field (old cache format)
        every { deviceCache.getOfferingsResponseCache() } returns JSONObject(ONE_OFFERINGS_RESPONSE)
        offeringsCache.cacheOfferings(originalOfferings, offeringsJson)

        // Retrieve from cache - should use cached instance's originalSource
        val cachedOfferings = offeringsCache.cachedOfferings

        assertThat(cachedOfferings).isNotNull
        assertThat(cachedOfferings!!.originalSource).isEqualTo(OriginalDataSource.MAIN)
        assertThat(cachedOfferings.source).isEqualTo(DataSource.MAIN)
    }

    @Test
    fun `LOAD_SHEDDER takes precedence over FALLBACK when both are set`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = true,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            httpResult.originalDataSource.asDataSource(),
        )

        // LOAD_SHEDDER should take precedence
        assertThat(offerings.originalSource).isEqualTo(OriginalDataSource.LOAD_SHEDDER)
        assertThat(offerings.source).isEqualTo(DataSource.LOAD_SHEDDER)
    }
}
