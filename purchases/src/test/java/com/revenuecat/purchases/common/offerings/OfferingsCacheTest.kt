package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.FakeLocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.utils.add
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsCacheTest {

    private val initialDate = Date(1685098228L) // Friday, May 26, 2023 10:50:28 AM GMT
    private lateinit var currentDate: Date

    private lateinit var deviceCache: DeviceCache
    private lateinit var dateProvider: DateProvider

    private lateinit var offeringsCache: OfferingsCache

    @Before
    fun setUp() {
        currentDate = initialDate
        deviceCache = mockk()
        dateProvider = object : DateProvider {
            override val now: Date
                get() = currentDate
        }

        offeringsCache = OfferingsCache(deviceCache, dateProvider = dateProvider)
    }

    @Test
    fun `clear cache clears offerings cache and offerings response cache`() {
        val offeringsResponse = mockk<JSONObject>()
        every { deviceCache.clearOfferingsResponseCache() } just Runs
        every { deviceCache.cacheOfferingsResponse(offeringsResponse) } just Runs
        offeringsCache.cacheOfferings(mockk(), offeringsResponse)
        assertThat(offeringsCache.cachedOfferings).isNotNull
        offeringsCache.clearCache()
        assertThat(offeringsCache.cachedOfferings).isNull()
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
        verify(exactly = 1) { deviceCache.clearOfferingsResponseCache() }
    }

    @Test
    fun `caching offerings works`() {
        val offerings = mockk<Offerings>()
        val offeringsResponse = mockk<JSONObject>()
        every { deviceCache.cacheOfferingsResponse(offeringsResponse) } just Runs
        assertThat(offeringsCache.cachedOfferings).isNull()
        offeringsCache.cacheOfferings(offerings, offeringsResponse)
        assertThat(offeringsCache.cachedOfferings).isEqualTo(offerings)
        verify(exactly = 1) { deviceCache.cacheOfferingsResponse(offeringsResponse) }
    }

    // region offerings cache

    @Test
    fun `cache is empty initially`() {
        assertThat(offeringsCache.cachedOfferings).isNull()
    }

    @Test
    fun `cache is stale if no cached value`() {
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(true)).isTrue
    }

    @Test
    fun `cache is not stale right after caching value`() {
        mockDeviceCacheOfferingResponse()
        offeringsCache.cacheOfferings(mockk(), mockk())
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isFalse
    }

    @Test
    fun `cache is stale if cached value is stale`() {
        mockDeviceCacheOfferingResponse()
        offeringsCache.cacheOfferings(mockk(), mockk())
        currentDate = currentDate.add(6.minutes)
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isTrue
    }

    @Test
    fun `cache is stale if cached value removes update time`() {
        mockDeviceCacheOfferingResponse()
        offeringsCache.cacheOfferings(mockk(), mockk())
        offeringsCache.clearOfferingsCacheTimestamp()
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isTrue
    }

    // endregion offerings cache

    // region locale cache tests

    @Test
    fun `cache is not stale when locales remain the same`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US", "es-ES")
        val offeringsCache = OfferingsCache(deviceCache, dateProvider = dateProvider, localeProvider = localeProvider)
        mockDeviceCacheOfferingResponse()

        // Act
        offeringsCache.cacheOfferings(mockk(), mockk())

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isFalse
    }

    @Test
    fun `cache is stale when locales change after caching`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US", "es-ES")
        val offeringsCache = OfferingsCache(deviceCache, dateProvider = dateProvider, localeProvider = localeProvider)
        mockDeviceCacheOfferingResponse()

        // Act
        offeringsCache.cacheOfferings(mockk(), mockk())
        localeProvider.languageTags = listOf("fr-FR", "de-DE")

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `cache is stale when a single locale changes`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US")
        val offeringsCache = OfferingsCache(deviceCache, dateProvider = dateProvider, localeProvider = localeProvider)
        mockDeviceCacheOfferingResponse()

        // Act
        offeringsCache.cacheOfferings(mockk(), mockk())
        localeProvider.languageTags = listOf("fr-FR")

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `cache is stale when locale order changes`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US", "es-ES")
        val offeringsCache = OfferingsCache(deviceCache, dateProvider = dateProvider, localeProvider = localeProvider)
        mockDeviceCacheOfferingResponse()

        // Act
        offeringsCache.cacheOfferings(mockk(), mockk())
        localeProvider.languageTags = listOf("es-ES", "en-US")

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `clear cache also clears cached locales`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US")
        val offeringsCache = OfferingsCache(deviceCache, dateProvider = dateProvider, localeProvider = localeProvider)
        mockDeviceCacheOfferingResponse()
        every { deviceCache.clearOfferingsResponseCache() } just Runs

        // Act
        offeringsCache.cacheOfferings(mockk(), mockk())
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isFalse
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isFalse
        offeringsCache.clearCache()
        
        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    // endregion locale cache tests

    // region offerings response cache

    @Test
    fun `offerings cache returns device cache offerings response`() {
        val offeringsResponse = mockk<JSONObject>()
        every { deviceCache.getOfferingsResponseCache() } returns offeringsResponse
        assertThat(offeringsCache.cachedOfferingsResponse).isEqualTo(offeringsResponse)
    }

    // endregion offerings response cache

    // region helpers

    fun mockDeviceCacheOfferingResponse() {
        every { deviceCache.cacheOfferingsResponse(any()) } just Runs
    }

    // endregion helpers
}
