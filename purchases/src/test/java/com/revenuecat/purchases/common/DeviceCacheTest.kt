//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfoOriginalSource
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.caching.CUSTOMER_INFO_SCHEMA_VERSION
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offlineentitlements.createProductEntitlementMapping
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.subtract
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrenciesFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class DeviceCacheTest {

    private val validCachedCustomerInfo by lazy {
        JSONObject(Responses.validFullPurchaserResponse).apply {
            put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
            put("verification_result", VerificationResult.VERIFIED)
        }.toString()
    }
    private val sampleProductEntitlementMapping = createProductEntitlementMapping()

    private val oldCachedCustomerInfo =
        "{'schema_version': 0, 'request_date': '2018-10-19T02:40:36Z', 'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z', 'purchase_date': '2018-10-26T23:17:53Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}"

    private lateinit var cache: DeviceCache
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockDateProvider: DateProvider
    private val apiKey = "api_key"
    private val appUserID = "app_user_id"
    private val currentTime = Date()

    private val productEntitlementMappingLastUpdatedCacheKey = "com.revenuecat.purchases.api_key.productEntitlementMappingLastUpdated"
    private val productEntitlementMappingCacheKey = "com.revenuecat.purchases.api_key.productEntitlementMapping"
    private val offeringsResponseCacheKey = "com.revenuecat.purchases.api_key.offeringsResponse"

    private val slotForPutLong = slot<Long>()

    @Before
    public fun setup() {
        mockkObject(VirtualCurrenciesFactory)
        mockPrefs = mockk()
        mockEditor = mockk()
        mockDateProvider = mockk()

        every {
            mockEditor.putString(any(), any())
        } returns mockEditor
        every {
            mockEditor.putLong(any(), capture(slotForPutLong))
        } returns mockEditor
        every {
            mockEditor.remove(any())
        } returns mockEditor

        every {
            mockPrefs.edit()
        } returns mockEditor

        every {
            mockEditor.apply()
        } just runs

        every { mockDateProvider.now } returns currentTime

        cache = DeviceCache(mockPrefs, apiKey, dateProvider = mockDateProvider)
    }

    @After
    public fun tearDown() {
        unmockkObject(VirtualCurrenciesFactory)
    }

    @Test
    fun `cache is created properly`() {
        assertThat(cache).`as`("cache is not null").isNotNull
    }

    @Test
    fun `given no cached info, cached purchased info is null`() {
        mockString(cache.customerInfoCacheKey(appUserID), null)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a customer info, the key in the cache is correct`() {
        mockString(cache.customerInfoCacheKey(appUserID), Responses.validFullPurchaserResponse)
        cache.getCachedCustomerInfo(appUserID)
        verify {
            mockPrefs.getString(cache.customerInfoCacheKey(appUserID), isNull())
        }
    }

    @Test
    fun `given a valid customer info, the JSON is parsed correctly`() {
        mockString(cache.customerInfoCacheKey(appUserID), validCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.entitlements?.verification).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `given a valid customer info without verification result, the JSON is parsed correctly`() {
        public val deprecatedValidCachedCustomerInfo by lazy {
            JSONObject(Responses.validFullPurchaserResponse).apply {
                put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
            }.toString()
        }
        mockString(cache.customerInfoCacheKey(appUserID), deprecatedValidCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.entitlements?.verification).isEqualTo(VerificationResult.NOT_REQUESTED)
    }

    @Test
    fun `given a valid customer info without request date, the JSON is parsed correctly`() {
        public val deprecatedValidCachedCustomerInfo by lazy {
            JSONObject(Responses.validFullPurchaserResponse).apply {
                put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
            }.toString()
        }
        mockString(cache.customerInfoCacheKey(appUserID), deprecatedValidCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.requestDate?.time).isEqualTo(1565951442000L)
    }

    @Test
    fun `given a valid customer info with schema version, the JSON is parsed correctly`() {
        public val deprecatedValidCachedCustomerInfo by lazy {
            JSONObject(Responses.validFullPurchaserResponse).apply {
                put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
                put("customer_info_request_date", 1234567890L)
            }.toString()
        }
        mockString(cache.customerInfoCacheKey(appUserID), deprecatedValidCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.schemaVersion).isEqualTo(CUSTOMER_INFO_SCHEMA_VERSION)
    }

    @Test
    fun `given a valid customer info with request date, the JSON is parsed correctly`() {
        public val deprecatedValidCachedCustomerInfo by lazy {
            JSONObject(Responses.validFullPurchaserResponse).apply {
                put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
                put("customer_info_request_date", 1234567890L)
            }.toString()
        }
        mockString(cache.customerInfoCacheKey(appUserID), deprecatedValidCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.requestDate?.time).isEqualTo(1234567890L)
    }

    @Test
    fun `given a invalid customer info, the information is null`() {
        mockString(cache.customerInfoCacheKey(appUserID), "not json")
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a valid customer info, the created customer info does not have verification result information`() {
        mockString(cache.customerInfoCacheKey(appUserID), validCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info?.rawData?.has("verification_result")).isFalse
    }

    @Test
    fun `given a customer info, the information is cached`() {
        val info = createCustomerInfo(Responses.validFullPurchaserResponse)

        cache.cacheCustomerInfo(appUserID, info)
        assertThat(slotForPutLong.captured).isNotNull
        verifyAll {
            mockEditor.putString(cache.customerInfoCacheKey(appUserID), any())
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey(appUserID), slotForPutLong.captured)
            mockEditor.apply()
        }
    }

    @Test
    fun `given a customer info, the information is cached with a verification result`() {
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey(appUserID), any())
        } returns mockEditor

        val info = createCustomerInfo(Responses.validFullPurchaserResponse, null, VerificationResult.VERIFIED)
        val infoJSONSlot = slot<String>()

        every {
            mockEditor.putString(any(), capture(infoJSONSlot))
        } returns mockEditor

        cache.cacheCustomerInfo(appUserID, info)

        val cachedJSON = JSONObject(infoJSONSlot.captured)
        assertThat(cachedJSON.has("verification_result")).isTrue
        assertThat(cachedJSON.getString("verification_result")).isEqualTo(VerificationResult.VERIFIED.name)
    }

    @Test
    fun `given a customer info, the information is cached with a schema version`() {
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey(appUserID), any())
        } returns mockEditor

        val info = createCustomerInfo(Responses.validFullPurchaserResponse)
        val infoJSONSlot = slot<String>()

        every {
            mockEditor.putString(any(), capture(infoJSONSlot))
        } returns mockEditor
        cache.cacheCustomerInfo(appUserID, info)

        val cachedJSON = JSONObject(infoJSONSlot.captured)
        assertThat(cachedJSON.has("schema_version")).isTrue
        assertThat(cachedJSON.getInt("schema_version")).isEqualTo(CUSTOMER_INFO_SCHEMA_VERSION)
    }

    @Test
    fun `given a customer info, the information is cached with a request date`() {
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey(appUserID), any())
        } returns mockEditor

        val info = createCustomerInfo(Responses.validFullPurchaserResponse)
        val infoJSONSlot = slot<String>()

        every {
            mockEditor.putString(any(), capture(infoJSONSlot))
        } returns mockEditor
        cache.cacheCustomerInfo(appUserID, info)

        val cachedJSON = JSONObject(infoJSONSlot.captured)
        assertThat(cachedJSON.has("customer_info_request_date")).isTrue
        assertThat(cachedJSON.getLong("customer_info_request_date")).isEqualTo(1565951442000L)
    }

    @Test
    fun `given an older version of customer info, nothing is returned`() {
        mockString(cache.customerInfoCacheKey(appUserID), oldCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a valid version customer info, it is returned`() {
        mockString(cache.customerInfoCacheKey(appUserID), validCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
    }

    @Test
    fun `given a cached customer info, source is set to CACHE and originalSource is preserved`() {
        val cachedInfoWithSource = JSONObject(Responses.validFullPurchaserResponse).apply {
            put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
            put("verification_result", VerificationResult.VERIFIED.name)
            put("customer_info_original_source", CustomerInfoOriginalSource.LOAD_SHEDDER.name)
        }.toString()
        mockString(cache.customerInfoCacheKey(appUserID), cachedInfoWithSource)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.originalSource).isEqualTo(CustomerInfoOriginalSource.LOAD_SHEDDER)
        assertThat(info?.loadedFromCache).isTrue
    }

    @Test
    fun `given a cached customer info without source fields, defaults are used`() {
        mockString(cache.customerInfoCacheKey(appUserID), validCachedCustomerInfo)
        val info = cache.getCachedCustomerInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
        assertThat(info?.originalSource).isEqualTo(CustomerInfoOriginalSource.MAIN)
        assertThat(info?.loadedFromCache).isTrue
    }

    @Test
    fun `cacheCustomerInfo stores originalSource and sets source to CACHE`() {
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey(appUserID), any())
        } returns mockEditor

        val info = createCustomerInfo(Responses.validFullPurchaserResponse)
        val infoJSONSlot = slot<String>()

        every {
            mockEditor.putString(any(), capture(infoJSONSlot))
        } returns mockEditor
        cache.cacheCustomerInfo(appUserID, info)

        val cachedJSON = JSONObject(infoJSONSlot.captured)
        assertThat(cachedJSON.has("customer_info_original_source")).isTrue
        assertThat(cachedJSON.getString("customer_info_original_source")).isEqualTo(info.originalSource.name)
    }

    @Test
    fun `given a non cached appuserid, the cached appuserid is null`() {
        mockString(cache.appUserIDCacheKey, null)
        val appUserID = cache.getCachedAppUserID()
        assertThat(appUserID).`as`("appUserID is null").isNull()
    }

    @Test
    fun `given a non cached appuserid, the cached appuserid is returned`() {
        mockString(cache.appUserIDCacheKey, appUserID)
        val returnedAppUserID = cache.getCachedAppUserID()
        assertThat(returnedAppUserID).`as`("appUserID is the same as the cached appUserID").isEqualTo(appUserID)
    }

    @Test
    fun `given a appuserid, it is able to cache it`() {
        every {
            mockEditor.apply()
        } just runs
        cache.cacheAppUserID(appUserID)
        verify {
            mockEditor.putString(cache.appUserIDCacheKey, any())
            mockEditor.apply()
        }
    }

    @Test
    fun `getting sent tokens works`() {
        val tokens = setOf("token1", "token2")
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } returns tokens
        val sentTokens = cache.getPreviouslySentHashedTokens()
        assertThat(sentTokens).isEqualTo(tokens)
    }

    @Test
    fun `token is hashed then added`() {
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } returns setOf("token1", "token2")
        val sha1 = "token3".sha1()
        every {
            mockEditor.putStringSet(cache.tokensCacheKey, setOf("token1", "token2", sha1))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs

        cache.addSuccessfullyPostedToken("token3")
        verify {
            mockEditor.putStringSet(cache.tokensCacheKey, setOf("token1", "token2", sha1))
        }
    }

    @Test
    fun `if token is not active anymore, remove it from database`() {
        every {
            mockEditor.putStringSet(cache.tokensCacheKey, setOf("token3"))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } returns setOf("token1", "token2", "token3")
        cache.cleanPreviouslySentTokens(setOf("token3", "token4"))
        verify {
            mockEditor.putStringSet(cache.tokensCacheKey, setOf("token3"))
        }
    }

    @Test
    fun `if all tokens are active, do not remove any`() {
        every {
            mockEditor.putStringSet(cache.tokensCacheKey, setOf("token1", "token2"))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } returns setOf("token1", "token2")
        cache.cleanPreviouslySentTokens(setOf("token1", "token2"))
        verify {
            mockEditor.putStringSet(cache.tokensCacheKey, setOf("token1", "token2"))
        }
    }

    @Test
    fun `getting the tokens not in cache returns all the active tokens that have not been sent`() {
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } returns setOf("token1", "hash2", "token3")
        val activeSub = mockk<StoreTransaction>(relaxed = true).also {
            every { it.type } returns ProductType.SUBS
        }
        val inApp = mockk<StoreTransaction>(relaxed = true).also {
            every { it.type } returns ProductType.INAPP
        }
        val activePurchasesNotInCache =
            cache.getActivePurchasesNotInCache(mapOf("hash1" to activeSub, "hash2" to inApp))
        assertThat(activePurchasesNotInCache).contains(activeSub)
    }

    @Test
    fun `invalidating customer info caches`() {
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = false)).isTrue
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = false)).isFalse
        cache.clearCustomerInfoCacheTimestamp(appUserID)
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), 0L)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = false)).isTrue
    }

    @Test
    fun `clearing caches clears all user ID data`() {
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey("appUserID"), capture(slot()))
        } returns mockEditor
        every {
            mockEditor.putLong(cache.virtualCurrenciesLastUpdatedCacheKey("appUserID"), capture(slot()))
        } returns mockEditor
        mockString(cache.appUserIDCacheKey, "appUserID")
        mockString(cache.legacyAppUserIDCacheKey, "legacyAppUserID")
        mockString(cache.customerInfoCacheKey(appUserID), null)
        mockString(cache.virtualCurrenciesCacheKey(appUserID), null)

        cache.clearCachesForAppUserID("appUserID")

        verify { mockEditor.remove(cache.appUserIDCacheKey) }
        verify { mockEditor.remove(cache.legacyAppUserIDCacheKey) }
        verify { mockEditor.remove(cache.customerInfoCacheKey("appUserID")) }
        verify { mockEditor.remove(cache.customerInfoCacheKey("legacyAppUserID")) }
        verify { mockEditor.remove(cache.virtualCurrenciesCacheKey("appUserID")) }
        verify { mockEditor.remove(cache.virtualCurrenciesCacheKey("legacyAppUserID")) }
        verify { mockEditor.remove(cache.virtualCurrenciesLastUpdatedCacheKey("appUserID")) }
        verify { mockEditor.remove(cache.virtualCurrenciesLastUpdatedCacheKey("legacyAppUserID")) }
    }

    @Test
    fun `clearing caches clears timestamps`() {
        val date = Date()
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey("appUserID"), date.time)
        } returns mockEditor
        cache.setCustomerInfoCacheTimestamp("appUserID", date)

        mockString(cache.appUserIDCacheKey, "appUserID")
        mockString(cache.legacyAppUserIDCacheKey, "legacyAppUserID")
        mockString(cache.customerInfoCacheKey(appUserID), null)

        cache.clearCachesForAppUserID("appUserID")
        verify {
            mockEditor.remove(cache.customerInfoLastUpdatedCacheKey("appUserID"))
        }
    }

    @Test
    fun `stale if no caches`() {
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), 0L)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = false)).isTrue
    }

    @Test
    fun `isCustomerInfoCacheStale returns true if the cached object is stale`() {
        cache.cacheCustomerInfo(appUserID, mockk(relaxed = true))
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = false)).isTrue
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = false)).isFalse
    }

    @Test
    fun `isCustomerInfoCacheStale in background returns true if the cached object is stale`() {
        cache.cacheCustomerInfo(appUserID, mockk(relaxed = true))
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = true)).isTrue
        mockLong(cache.customerInfoLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isCustomerInfoCacheStale(appUserID, appInBackground = true)).isFalse
    }

    @Test
    fun `timestamp is set when caching customer info`() {
        cache.cacheCustomerInfo("waldo", mockk(relaxed = true))
        assertThat(slotForPutLong.captured).isNotNull
    }

    @Test
    fun `clearing customer info caches clears the shared preferences`() {
        cache.cacheCustomerInfo(appUserID, mockk(relaxed = true))
        assertThat(slotForPutLong.captured).isNotNull

        cache.clearCustomerInfoCache(appUserID)
        verify { mockEditor.remove(cache.customerInfoCacheKey(appUserID)) }
    }

    @Test
    fun `getPreviouslySentHashedTokens returns an emptySet if there's a ClassCastException when calling getStringSet`() {
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } throws ClassCastException("java.lang.String cannot be cast to java.util.Set")
        val sentTokens = cache.getPreviouslySentHashedTokens()
        assertThat(sentTokens).isEmpty()
    }

    @Test
    fun `If getting all preferences throws NullPointerException when calling findKeysThatStartWith, an empty set is returned`() {
        every {
            mockPrefs.all
        } throws NullPointerException("NullPointerException")

        val returnedSetOfKeys = cache.findKeysThatStartWith("any_cache_key")
        assertThat(returnedSetOfKeys).isEmpty()
    }

    @Test
    fun `cleanupOldAttributionData cleans all old caches`() {
        val stubPreferences = mapOf(
            "${cache.attributionCacheKey}.cesar.facebook" to "facebookid",
            "${cache.attributionCacheKey}.cesar.tenjin" to "tenjinid",
            "${cache.attributionCacheKey}.pedro.mixpanel" to "mixpanelid",
        )
        every {
            mockPrefs.all
        } returns stubPreferences

        cache.cleanupOldAttributionData()

        stubPreferences.keys.forEach { verify (exactly = 1) { mockEditor.remove(it) } }

        verify (exactly = 1) { mockEditor.apply() }
    }

    @Test
    fun `cleanupOldAttributionData doesn't crash if a null key is stored in SharedPreferences`() {
        val stubPreferences = mapOf(
            null to "random-value",
            "${cache.attributionCacheKey}.cesar.tenjin" to "tenjinid",
            "${cache.attributionCacheKey}.pedro.mixpanel" to "mixpanelid",
        )
        every {
            mockPrefs.all
        } returns stubPreferences

        cache.cleanupOldAttributionData()

        verify (exactly = 1) { mockEditor.remove("${cache.attributionCacheKey}.cesar.tenjin") }
        verify (exactly = 1) { mockEditor.remove("${cache.attributionCacheKey}.pedro.mixpanel") }
        verify (exactly = 2) { mockEditor.remove(any()) }

        verify (exactly = 1) { mockEditor.apply() }
    }

    // region ProductEntitlementMapping

    @Test
    fun `cacheProductEntitlementMapping caches mappings in shared preferences correctly`() {
        cache.cacheProductEntitlementMapping(sampleProductEntitlementMapping)
        verify(exactly = 1) {
            mockEditor.putString(
                productEntitlementMappingCacheKey,
                "{\"product_entitlement_mapping\":{\"com.revenuecat.foo_1:p1m\":{\"product_identifier\":\"com.revenuecat.foo_1\",\"base_plan_id\":\"p1m\",\"entitlements\":[\"pro_1\"]},\"com.revenuecat.foo_1:p1y\":{\"product_identifier\":\"com.revenuecat.foo_1\",\"base_plan_id\":\"p1y\",\"entitlements\":[\"pro_1\",\"pro_2\"]},\"com.revenuecat.foo_1\":{\"product_identifier\":\"com.revenuecat.foo_1\",\"base_plan_id\":\"p1m\",\"entitlements\":[\"pro_1\"]},\"com.revenuecat.foo_2\":{\"product_identifier\":\"com.revenuecat.foo_2\",\"entitlements\":[\"pro_3\"]}},\"rc_original_source\":\"MAIN\"}"
            )
        }
        verify(exactly = 1) {
            mockEditor.putLong(productEntitlementMappingLastUpdatedCacheKey, currentTime.time)
        }
        verify(exactly = 2) { mockEditor.apply() }
    }

    @Test
    fun `cacheProductEntitlementMapping caches empty mappings in shared preferences correctly`() {
        cache.cacheProductEntitlementMapping(createProductEntitlementMapping(emptyMap()))
        verify(exactly = 1) {
            mockEditor.putString(
                productEntitlementMappingCacheKey,
                "{\"product_entitlement_mapping\":{},\"rc_original_source\":\"MAIN\"}",
            )
        }
        verify(exactly = 1) {
            mockEditor.putLong(productEntitlementMappingLastUpdatedCacheKey, currentTime.time)
        }
        verify(exactly = 2) { mockEditor.apply() }
    }

    @Test
    fun `setProductEntitlementMappingCacheTimestampToNow caches cache timestamp correctly`() {
        cache.setProductEntitlementMappingCacheTimestampToNow()
        verify(exactly = 1) {
            mockEditor.putLong(productEntitlementMappingLastUpdatedCacheKey, currentTime.time)
        }
        verify(exactly = 1) { mockEditor.apply() }
    }

    @Test
    fun `isProductEntitlementMappingCacheStale returns stale if nothing in cache`() {
        every {
            mockPrefs.contains(productEntitlementMappingLastUpdatedCacheKey)
        } returns false
        assertThat(cache.isProductEntitlementMappingCacheStale()).isTrue
    }

    @Test
    fun `isProductEntitlementMappingCacheStale returns stale if cache older than cache period`() {
        every {
            mockPrefs.contains(productEntitlementMappingLastUpdatedCacheKey)
        } returns true
        every {
            mockPrefs.getLong(productEntitlementMappingLastUpdatedCacheKey, any())
        } returns currentTime.subtract(25.hours + 1.minutes).time
        assertThat(cache.isProductEntitlementMappingCacheStale()).isTrue
    }

    @Test
    fun `isProductEntitlementMappingCacheStale returns not stale if cache newer than cache period`() {
        every {
            mockPrefs.contains(productEntitlementMappingLastUpdatedCacheKey)
        } returns true
        every {
            mockPrefs.getLong(productEntitlementMappingLastUpdatedCacheKey, any())
        } returns currentTime.subtract(25.hours - 1.minutes).time
        assertThat(cache.isProductEntitlementMappingCacheStale()).isFalse
    }

    @Test
    fun `getProductEntitlementMapping returns null if nothing in cache`() {
        every { mockPrefs.getString(productEntitlementMappingCacheKey, null) } returns null
        assertThat(cache.getProductEntitlementMapping()).isNull()
    }

    @Test
    fun `getProductEntitlementMapping returns null if cache has invalid value`() {
        every {
            mockPrefs.getString(productEntitlementMappingCacheKey, null)
        } returns "invalid-json"
        assertThat(cache.getProductEntitlementMapping()).isNull()
    }

    @Test
    fun `getProductEntitlementMapping clears cache if cache has invalid value`() {
        every {
            mockPrefs.getString(productEntitlementMappingCacheKey, null)
        } returns "invalid-json"
        every {
            mockEditor.remove(productEntitlementMappingCacheKey)
        } returns mockEditor
        cache.getProductEntitlementMapping()
        verify(exactly = 1) { mockEditor.remove(productEntitlementMappingCacheKey) }
        verify(exactly = 1) { mockEditor.apply() }
    }

    @Test
    fun `getProductEntitlementMapping returns correct product entitlements mapping from cache`() {
        val expectedMappings = createProductEntitlementMapping()
        every {
            mockPrefs.getString(productEntitlementMappingCacheKey, null)
        } returns expectedMappings.toJson().toString()
        assertThat(cache.getProductEntitlementMapping()).isEqualTo(expectedMappings.copy(loadedFromCache = true))
    }

    // endregion

    // region offerings response

    @Test
    fun `gets offerings from shared preferences`() {
        every { mockPrefs.getString(offeringsResponseCacheKey, null) } returns "{\"test-key\": \"test-value\"}"
        val offeringsResponse = cache.getOfferingsResponseCache()
        assertThat(offeringsResponse).isNotNull
        assertThat(offeringsResponse?.getString("test-key")).isEqualTo("test-value")
    }

    @Test
    fun `cache offerings response works`() {
        val jsonSample = "{\"test-key\":\"test-value\"}"
        val offeringsResponse = JSONObject(jsonSample)
        cache.cacheOfferingsResponse(offeringsResponse)
        verifyAll {
            mockEditor.putString(offeringsResponseCacheKey, jsonSample)
            mockEditor.apply()
        }
    }

    @Test
    fun `clear offerings response cache works`() {
        cache.clearOfferingsResponseCache()
        verifyAll {
            mockEditor.remove(offeringsResponseCacheKey)
            mockEditor.apply()
        }
    }

    // endregion offerings response

    // region storefront

    @Test
    fun `setStorefront saves storefront in cache`() {
        val storefront = "JP"
        cache.setStorefront(storefront)
        verifyAll {
            mockEditor.putString(cache.storefrontCacheKey, storefront)
            mockEditor.apply()
        }
    }

    @Test
    fun `getStorefront gets storefront from cache`() {
        val storefront = "JP"
        every { mockPrefs.getString(cache.storefrontCacheKey, null) } returns storefront
        val cachedStorefront = cache.getStorefront()
        assertThat(cachedStorefront).isNotNull
        assertThat(cachedStorefront).isEqualTo(storefront)
    }

    // endregion storefront

    // region virtualCurrencies
    @Test
    fun `given no cached virtual currencies, cached virtual currencies is null`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), null)
        val info = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a virtual currencies, the key in the cache is correct`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), Responses.validFullVirtualCurrenciesResponse)
        cache.getCachedVirtualCurrencies(appUserID)
        verify {
            mockPrefs.getString(cache.virtualCurrenciesCacheKey(appUserID), isNull())
        }
    }

    @Test
    fun `given a valid VirtualCurrencies, the JSON is parsed correctly`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), Responses.validFullVirtualCurrenciesResponse)
        val vcs = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(vcs).`as`("vcs is not null").isNotNull
        assertThat(vcs?.all?.size).isEqualTo(2)
    }

    @Test
    fun `given an invalid VirtualCurrencies JSON string, the returned VirtualCurrencies from the cache is null`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), "not json")
        val vcs = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(vcs).`as`("vcs is null").isNull()
    }

    @Test
    fun `given a VirtualCurrencies, the VirtualCurrencies is cached`() {
        val vcs = VirtualCurrenciesFactory.buildVirtualCurrencies(
            JSONObject(Responses.validFullVirtualCurrenciesResponse)
        )

        cache.cacheVirtualCurrencies(appUserID, vcs)
        assertThat(slotForPutLong.captured).isNotNull
        
        // The serializer caches the JSON fields in a different order than the network response. Here, we
        // check for the expected order.
        val expectedCachedJSON = "{\"virtual_currencies\":{\"COIN\":{\"balance\":1,\"name\":\"Coin\",\"code\":" +
            "\"COIN\",\"description\":\"It's a coin\"},\"RC_COIN\":{\"balance\":0,\"name\":\"RC Coin\",\"code\"" +
            ":\"RC_COIN\"}}}"
        verifyAll {
            mockEditor.putString(cache.virtualCurrenciesCacheKey(appUserID), expectedCachedJSON)
            mockEditor.putLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), slotForPutLong.captured)
            mockEditor.apply()
        }
    }

    @Test
    fun `invalidating VirtualCurrencies caches`() {
        mockLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isVirtualCurrenciesCacheStale(appUserID, appInBackground = false)).isTrue

        mockLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isVirtualCurrenciesCacheStale(appUserID, appInBackground = false)).isFalse

        mockString(cache.appUserIDCacheKey, appUserID)
        mockString(cache.legacyAppUserIDCacheKey, null)

        cache.clearVirtualCurrenciesCache(appUserID)
        verify {
            mockEditor.remove(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID))
            mockEditor.remove(cache.virtualCurrenciesCacheKey(appUserID))
        }

        mockLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), 0L)
        assertThat(cache.isVirtualCurrenciesCacheStale(appUserID, appInBackground = false)).isTrue
    }

    @Test
    fun `isVirtualCurrenciesCacheStale returns true if the cached object is stale`() {
        mockLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isVirtualCurrenciesCacheStale(appUserID, appInBackground = false)).isTrue

        mockLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isVirtualCurrenciesCacheStale(appUserID, appInBackground = false)).isFalse
    }

    @Test
    fun `cached VirtualCurrencies are equal to provided value`() {
        val expectedVirtualCurrencies = VirtualCurrenciesFactory.buildVirtualCurrencies(
            JSONObject(Responses.validFullVirtualCurrenciesResponse)
        )
        mockLong(cache.virtualCurrenciesLastUpdatedCacheKey(appUserID), Date().time)
        mockString(cache.appUserIDCacheKey, appUserID)
        mockString(cache.legacyAppUserIDCacheKey, null)
        mockString(cache.virtualCurrenciesCacheKey(appUserID), Responses.validFullVirtualCurrenciesResponse)

        cache.cacheVirtualCurrencies(appUserID, expectedVirtualCurrencies)
        val cachedVirtualCurrencies = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(cachedVirtualCurrencies).isEqualTo(expectedVirtualCurrencies)

        // The serializer caches the JSON fields in a different order than the network response. Here, we
        // check for the expected order.
        val expectedCachedJSON = "{\"virtual_currencies\":{\"COIN\":{\"balance\":1,\"name\":\"Coin\",\"code\":" +
            "\"COIN\",\"description\":\"It's a coin\"},\"RC_COIN\":{\"balance\":0,\"name\":\"RC Coin\",\"code\"" +
            ":\"RC_COIN\"}}}"
        verify {
            mockEditor.putString(
                cache.virtualCurrenciesCacheKey(appUserID),
                expectedCachedJSON
            )
        }
    }

    @Test
    fun `getCachedVirtualCurrencies returns null when VirtualCurrenciesFactory throws SerializationException`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), "{}")
        
        every {
            VirtualCurrenciesFactory.buildVirtualCurrencies(jsonString = any())
        } throws SerializationException("Serialization error")
        
        val vcs = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(vcs).`as`("cached VirtualCurrencies is null when SerializationException is thrown").isNull()
    }

    @Test
    fun `getCachedVirtualCurrencies returns null when VirtualCurrenciesFactory throws IllegalArgumentException`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), "{}")
        
        every {
            VirtualCurrenciesFactory.buildVirtualCurrencies(jsonString = any())
        } throws IllegalArgumentException("Invalid input")
        
        val vcs = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(vcs).`as`("cached VirtualCurrencies is null when IllegalArgumentException is thrown").isNull()
    }

    @Test
    fun `getCachedVirtualCurrencies returns null when VirtualCurrenciesFactory throws JSONException`() {
        mockString(cache.virtualCurrenciesCacheKey(appUserID), "{}")

        every {
            VirtualCurrenciesFactory.buildVirtualCurrencies(jsonString = any())
        } throws JSONException("JSON exception")

        val vcs = cache.getCachedVirtualCurrencies(appUserID)
        assertThat(vcs).`as`("cached VirtualCurrencies is null when JSONException is thrown").isNull()
    }
    // endregion virtualCurrencies

    private fun mockString(key: String, value: String?) {
        every {
            mockPrefs.getString(eq(key), isNull())
        } returns value
    }

    private fun mockLong(key: String, value: Long) {
        every {
            mockPrefs.getLong(eq(key), 0L)
        } returns value
    }
}
