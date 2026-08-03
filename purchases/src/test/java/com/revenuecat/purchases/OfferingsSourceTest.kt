package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
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
import io.mockk.verify
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
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            loadedFromDiskCache = false,
        )

        assertThat(offerings.originalSource).isEqualTo(HTTPResponseOriginalSource.MAIN)
        assertThat(offerings.loadedFromDiskCache).isFalse
    }

    @Test
    fun `offerings created with LOAD_SHEDDER source from response with header`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = false,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            loadedFromDiskCache = false,
        )

        assertThat(offerings.originalSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
        assertThat(offerings.loadedFromDiskCache).isFalse
    }

    @Test
    fun `offerings created with FALLBACK source from fallback URL response`() {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = false,
            isFallbackURL = true,
        )
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        val offerings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            loadedFromDiskCache = false,
        )

        assertThat(offerings.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
        assertThat(offerings.loadedFromDiskCache).isFalse
    }

    @Test
    fun `caching offerings stores the response body as received`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create offerings with LOAD_SHEDDER source
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = true,
            isFallbackURL = false,
        )
        val originalOfferings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            loadedFromDiskCache = false,
        )

        // Cache the offerings
        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        offeringsCache.cacheOfferings(originalOfferings, ONE_OFFERINGS_RESPONSE)

        verify(exactly = 1) {
            deviceCache.cacheOfferingsResponse(ONE_OFFERINGS_RESPONSE)
        }
    }

    @Test
    fun `offerings cache preserves originalSource when reading from cache`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create and cache offerings with FALLBACK source
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.BACKEND,
            isLoadShedderResponse = false,
            isFallbackURL = true,
        )
        val originalOfferings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            httpResult.originalDataSource,
            loadedFromDiskCache = false,
        )

        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        offeringsCache.cacheOfferings(originalOfferings, ONE_OFFERINGS_RESPONSE)

        // Retrieve from cache - originalSource should be preserved
        val cachedOfferings = offeringsCache.cachedOfferings

        assertThat(cachedOfferings).isNotNull
        assertThat(cachedOfferings!!.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
        assertThat(cachedOfferings.loadedFromDiskCache).isFalse
    }

    @Test
    fun `offerings source is null when not known`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Null rather than MAIN: a caller that does not say where the response came from must not have
        // the main API asserted on its behalf. The disk-cache path is the real case.
        val offerings = offeringParser.createOfferings(offeringsJson, productsById)

        assertThat(offerings.originalSource).isNull()
        assertThat(offerings.loadedFromDiskCache).isFalse
    }

    @Test
    fun `the cached offerings instance keeps the source it was built with`() {
        val offeringsJson = JSONObject(ONE_OFFERINGS_RESPONSE)
        val productsById = mapOf(STUB_PRODUCT_IDENTIFIER to listOf(stubStoreProduct(STUB_PRODUCT_IDENTIFIER)))

        // Create offerings with MAIN source
        val originalOfferings = offeringParser.createOfferings(
            offeringsJson,
            productsById,
            HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = true,
        )

        every { deviceCache.cacheOfferingsResponse(any()) } returns Unit
        offeringsCache.cacheOfferings(originalOfferings, ONE_OFFERINGS_RESPONSE)

        val cachedOfferings = offeringsCache.cachedOfferings

        assertThat(cachedOfferings).isNotNull
        assertThat(cachedOfferings!!.originalSource).isEqualTo(HTTPResponseOriginalSource.MAIN)
        assertThat(cachedOfferings.loadedFromDiskCache).isTrue
    }

    @Test
    fun `FALLBACK takes precedence over LOAD_SHEDDER when both are set`() {
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
            loadedFromDiskCache = false,
        )

        assertThat(offerings.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
        assertThat(offerings.loadedFromDiskCache).isFalse
    }
}
