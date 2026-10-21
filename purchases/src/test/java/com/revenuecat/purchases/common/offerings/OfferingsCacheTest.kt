package com.revenuecat.purchases.common.offerings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FakeLocaleProvider
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.PayloadFileStore
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.add
import com.revenuecat.purchases.utils.copy
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsCacheTest {

    private val initialDate = Date(1685098228L) // Friday, May 26, 2023 10:50:28 AM GMT
    private lateinit var currentDate: Date

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val legacyOfferingsResponseKey = "com.revenuecat.purchases.api_key.offeringsResponse"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var responseDirectory: File
    private lateinit var deviceCache: DeviceCache
    private lateinit var dateProvider: DateProvider

    private lateinit var offeringsCache: OfferingsCache

    @Before
    fun setUp() {
        currentDate = initialDate
        sharedPreferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("offerings_cache_test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        responseDirectory = File(temporaryFolder.root, "offerings_response")
        deviceCache = DeviceCache(sharedPreferences, "api_key")
        dateProvider = object : DateProvider {
            override val now: Date
                get() = currentDate
        }

        offeringsCache = createOfferingsCache()
    }

    @Test
    fun `clear cache clears offerings cache and offerings response cache`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        assertThat(offeringsCache.cachedOfferings).isNotNull
        offeringsCache.clearCache()
        assertThat(offeringsCache.cachedOfferings).isNull()
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        assertThat(responseDirectory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `caching offerings works`() {
        val offerings = mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }
        assertThat(offeringsCache.cachedOfferings).isNull()
        offeringsCache.cacheOfferings(offerings, ONE_OFFERINGS_RESPONSE)
        assertThat(offeringsCache.cachedOfferings).isEqualTo(offerings)
        assertThat(offeringsCache.cachedOfferingsResponse.toString())
            .isEqualTo(JSONObject(ONE_OFFERINGS_RESPONSE).toString())
    }

    @Test
    fun `offerings fetched before a clearCache are not cached`() {
        val requestEpoch = offeringsCache.epoch

        offeringsCache.clearCache()
        offeringsCache.cacheOfferings(mockk(), ONE_OFFERINGS_RESPONSE, requestEpoch)

        assertThat(offeringsCache.cachedOfferings).isNull()
        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNull()
    }

    @Test
    fun `offerings fetched after a clearCache are cached`() {
        offeringsCache.clearCache()
        val offerings = mockk<Offerings>()

        offeringsCache.cacheOfferings(offerings, ONE_OFFERINGS_RESPONSE, offeringsCache.epoch)

        assertThat(offeringsCache.cachedOfferings).isEqualTo(offerings)
    }

    @Test
    fun `caching offerings from the disk cache does not write the response back to disk`() {
        val offerings = mockk<Offerings>()

        offeringsCache.cacheOfferings(offerings, responsePayload = null)

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        // The in-memory cache must still be populated, or every later getOfferings re-parses the disk blob.
        assertThat(offeringsCache.cachedOfferings).isEqualTo(offerings)
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isFalse
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
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isFalse
    }

    @Test
    fun `cache is stale if cached value is stale`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        currentDate = currentDate.add(6.minutes)
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isTrue
    }

    @Test
    fun `cache is stale if forced to be stale`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        offeringsCache.forceCacheStale()
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isTrue
    }

    @Test
    fun `clearInMemoryOfferingsCache clears cached offerings`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        assertThat(offeringsCache.cachedOfferings).isNotNull
        offeringsCache.clearInMemoryOfferingsCache()
        assertThat(offeringsCache.cachedOfferings).isNull()
    }

    @Test
    fun `clearInMemoryOfferingsCache makes cache stale`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isFalse
        offeringsCache.clearInMemoryOfferingsCache()
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(true)).isTrue
    }

    @Test
    fun `clearInMemoryOfferingsCache does not clear disk cache`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        offeringsCache.clearInMemoryOfferingsCache()
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNotNull
        assertThat(responseDirectory.listFiles().orEmpty()).isNotEmpty
    }

    @Test
    fun `clearInMemoryOfferingsCache preserves disk cache for fallback`() {
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)

        offeringsCache.clearInMemoryOfferingsCache()

        assertThat(offeringsCache.cachedOfferings).isNull()
        assertThat(offeringsCache.cachedOfferingsResponse).isNotNull
    }

    // endregion offerings cache

    // region locale cache tests

    @Test
    fun `cache is not stale when locales remain the same`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US", "es-ES")
        val offeringsCache = createOfferingsCache(localeProvider)

        // Act
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(false)).isFalse
    }

    @Test
    fun `cache is stale when locales change after caching`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US", "es-ES")
        val offeringsCache = createOfferingsCache(localeProvider)

        // Act
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        localeProvider.languageTags = listOf("fr-FR", "de-DE")

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `cache is stale when a single locale changes`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US")
        val offeringsCache = createOfferingsCache(localeProvider)

        // Act
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        localeProvider.languageTags = listOf("fr-FR")

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `cache is stale when locale order changes`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US", "es-ES")
        val offeringsCache = createOfferingsCache(localeProvider)

        // Act
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
        localeProvider.languageTags = listOf("es-ES", "en-US")

        // Assert
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = false)).isTrue
        assertThat(offeringsCache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `clear cache also clears cached locales`() {
        // Arrange
        val localeProvider = FakeLocaleProvider("en-US")
        val offeringsCache = createOfferingsCache(localeProvider)

        // Act
        offeringsCache.cacheOfferings(mockk<Offerings>().apply {
            every { originalSource } returns HTTPResponseOriginalSource.MAIN
        }, ONE_OFFERINGS_RESPONSE)
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
    fun `response is stored in a file and only its checksum in shared preferences`() {
        val responsePayload = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "payload-marker").toString()

        offeringsCache.cacheOfferings(mockk<Offerings>(), responsePayload)

        assertThat(responseDirectory.listFiles().orEmpty()).hasSize(1)
        assertThat(sharedPreferences.all.values.map { it.toString() })
            .noneMatch { it.contains("payload-marker") }
        assertThat(offeringsCache.cachedOfferingsResponse?.getString("marker")).isEqualTo("payload-marker")
    }

    @Test
    fun `response survives a new cache instance`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)

        val reloaded = createOfferingsCache()

        assertThat(reloaded.cachedOfferings).isNull()
        assertThat(reloaded.cachedOfferingsResponse.toString())
            .isEqualTo(JSONObject(ONE_OFFERINGS_RESPONSE).toString())
    }

    @Test
    fun `response write runs on the dispatcher`() {
        val dispatcher = CapturingDispatcher()
        val offeringsCache = createOfferingsCache(dispatcher = dispatcher)

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNull()
        assertThat(responseDirectory.listFiles().orEmpty()).isEmpty()

        dispatcher.runAll()
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNotNull
        assertThat(responseDirectory.listFiles().orEmpty()).hasSize(1)
    }

    @Test
    fun `a write enqueued before clearCache does not restore the response`() {
        val dispatcher = CapturingDispatcher()
        val offeringsCache = createOfferingsCache(dispatcher = dispatcher)

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        offeringsCache.clearCache()
        dispatcher.runAll()

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNull()
    }

    @Test
    fun `clearCache during an in-flight write does not restore the response`() {
        val responseStore = spyk(PayloadFileStore(responseDirectory))
        val offeringsCache = createOfferingsCache(responseStore = responseStore)
        every { responseStore.write(any(), any()) } answers {
            offeringsCache.clearCache()
            callOriginal()
        }

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNull()
        assertThat(responseDirectory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `a superseded write does not overwrite a newer response`() {
        val dispatcher = CapturingDispatcher()
        val offeringsCache = createOfferingsCache(dispatcher = dispatcher)
        val newerResponse = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "newer").toString()

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        offeringsCache.cacheOfferings(mockk<Offerings>(), newerResponse)
        dispatcher.runAll(reversed = true)

        assertThat(offeringsCache.cachedOfferingsResponse?.getString("marker")).isEqualTo("newer")
    }

    @Test
    fun `caching the stored response again does not rewrite the file`() {
        val responseStore = spyk(PayloadFileStore(responseDirectory))
        val offeringsCache = createOfferingsCache(responseStore = responseStore)

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        offeringsCache.cacheOfferings(mockk<Offerings>(), String(ONE_OFFERINGS_RESPONSE.toCharArray()))

        verify(exactly = 1) { responseStore.write(any(), any()) }
        assertThat(offeringsCache.cachedOfferingsResponse.toString())
            .isEqualTo(JSONObject(ONE_OFFERINGS_RESPONSE).toString())
    }

    @Test
    fun `caching a different response rewrites the file`() {
        val newerResponse = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "newer").toString()

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        offeringsCache.cacheOfferings(mockk<Offerings>(), newerResponse)

        assertThat(offeringsCache.cachedOfferingsResponse?.getString("marker")).isEqualTo("newer")
    }

    @Test
    fun `a failed write keeps the previous response`() {
        val responseStore = spyk(PayloadFileStore(responseDirectory))
        val offeringsCache = createOfferingsCache(responseStore = responseStore)
        val newerResponse = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "newer").toString()
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        val storedChecksum = deviceCache.getOfferingsResponseChecksum()
        every { responseStore.write(any(), newerResponse) } returns null

        offeringsCache.cacheOfferings(mockk<Offerings>(), newerResponse)

        assertThat(deviceCache.getOfferingsResponseChecksum()).isEqualTo(storedChecksum)
        assertThat(offeringsCache.cachedOfferingsResponse.toString())
            .isEqualTo(JSONObject(ONE_OFFERINGS_RESPONSE).toString())
    }

    @Test
    fun `caching the stored response again restores a missing file`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        responseDirectory.listFiles().orEmpty().single().delete()

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)

        assertThat(offeringsCache.cachedOfferingsResponse).isNotNull
    }

    @Test
    fun `a damaged file is rewritten by the next unchanged response`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        val responseFile = responseDirectory.listFiles().orEmpty().single()
        responseFile.writeText(ONE_OFFERINGS_RESPONSE.take(ONE_OFFERINGS_RESPONSE.length / 2))
        assertThat(offeringsCache.cachedOfferingsResponse).isNull()

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)

        assertThat(offeringsCache.cachedOfferingsResponse.toString())
            .isEqualTo(JSONObject(ONE_OFFERINGS_RESPONSE).toString())
    }

    @Test
    fun `a failed read does not clear the checksum of a newer response`() {
        val responseStore = spyk(PayloadFileStore(responseDirectory))
        val offeringsCache = createOfferingsCache(responseStore = responseStore)
        val newerResponse = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "newer").toString()
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        var newerWrittenDuringRead = false
        every { responseStore.read(any(), any()) } answers {
            if (!newerWrittenDuringRead) {
                newerWrittenDuringRead = true
                offeringsCache.cacheOfferings(mockk<Offerings>(), newerResponse)
            }
            callOriginal()
        }

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()

        assertThat(offeringsCache.cachedOfferingsResponse?.getString("marker")).isEqualTo("newer")
    }

    @Test
    fun `response is null when nothing was cached`() {
        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
    }

    @Test
    fun `response is null when the file does not match the stored checksum`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        responseDirectory.listFiles().orEmpty().single().writeText("{}")

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
    }

    @Test
    fun `response is null when the stored payload is not JSON`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), "not json")

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        assertThat(deviceCache.getOfferingsResponseChecksum()).isNull()
    }

    @Test
    fun `response is null when the checksum is missing`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        deviceCache.clearOfferingsResponseChecksum()

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
    }

    @Test
    fun `creating the cache drops the legacy response stored in shared preferences`() {
        sharedPreferences.edit().putString(legacyOfferingsResponseKey, ONE_OFFERINGS_RESPONSE).commit()

        val offeringsCache = createOfferingsCache()

        assertThat(sharedPreferences.contains(legacyOfferingsResponseKey)).isFalse
        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
    }

    @Test
    fun `a legacy response written by an older SDK invalidates the stored response`() {
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        sharedPreferences.edit().putString(legacyOfferingsResponseKey, ONE_OFFERINGS_RESPONSE).commit()

        val reloaded = createOfferingsCache()

        assertThat(reloaded.cachedOfferingsResponse).isNull()
    }

    @Test
    fun `responses for different API keys do not overwrite each other`() {
        val otherApiKeyCache = createOfferingsCache(
            deviceCache = DeviceCache(sharedPreferences, "other_api_key"),
        )
        val otherResponse = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "other").toString()

        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        otherApiKeyCache.cacheOfferings(mockk<Offerings>(), otherResponse)

        assertThat(offeringsCache.cachedOfferingsResponse?.has("marker")).isFalse
        assertThat(otherApiKeyCache.cachedOfferingsResponse?.getString("marker")).isEqualTo("other")
    }

    @Test
    fun `clearing the cache for one API key keeps another key's response`() {
        val otherApiKeyCache = createOfferingsCache(
            deviceCache = DeviceCache(sharedPreferences, "other_api_key"),
        )
        val otherResponse = JSONObject(ONE_OFFERINGS_RESPONSE).put("marker", "other").toString()
        offeringsCache.cacheOfferings(mockk<Offerings>(), ONE_OFFERINGS_RESPONSE)
        otherApiKeyCache.cacheOfferings(mockk<Offerings>(), otherResponse)

        offeringsCache.clearCache()

        assertThat(offeringsCache.cachedOfferingsResponse).isNull()
        assertThat(otherApiKeyCache.cachedOfferingsResponse?.getString("marker")).isEqualTo("other")
    }

    // endregion offerings response cache

    // region helpers

    private fun createOfferingsCache(
        localeProvider: LocaleProvider = DefaultLocaleProvider(),
        dispatcher: Dispatcher = SyncDispatcher(),
        responseStore: PayloadFileStore = PayloadFileStore(responseDirectory),
        deviceCache: DeviceCache = this.deviceCache,
    ) = OfferingsCache(
        deviceCache,
        responseStore = responseStore,
        dispatcher = dispatcher,
        dateProvider = dateProvider,
        localeProvider = localeProvider,
    )

    private class CapturingDispatcher : Dispatcher(mockk()) {
        private val commands = mutableListOf<Runnable>()

        override fun enqueue(command: Runnable, delay: Delay) {
            commands.add(command)
        }

        fun runAll(reversed: Boolean = false) {
            val toRun = if (reversed) commands.reversed() else commands.toList()
            commands.clear()
            toRun.forEach { it.run() }
        }
    }

    // endregion helpers
}
