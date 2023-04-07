//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.caching.CUSTOMER_INFO_SCHEMA_VERSION
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.offlineentitlements.createProductEntitlementMapping
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.subtract
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DeviceCacheTest {

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

    private val slotForPutLong = slot<Long>()

    @Before
    fun setup() {
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
        // Trusted entitlements: Commented out until ready to be made public
        // assertThat(info?.entitlements?.verification).isEqualTo(VerificationResult.VERIFIED)
    }

    // Trusted entitlements: Commented out until ready to be made public
//    @Test
//    fun `given a valid customer info without verification result, the JSON is parsed correctly`() {
//        val deprecatedValidCachedCustomerInfo by lazy {
//            JSONObject(Responses.validFullPurchaserResponse).apply {
//                put("schema_version", CUSTOMER_INFO_SCHEMA_VERSION)
//            }.toString()
//        }
//        mockString(cache.customerInfoCacheKey(appUserID), deprecatedValidCachedCustomerInfo)
//        val info = cache.getCachedCustomerInfo(appUserID)
//        assertThat(info).`as`("info is not null").isNotNull
//        assertThat(info?.entitlements?.verification).isEqualTo(VerificationResult.NOT_REQUESTED)
//    }

    @Test
    fun `given a valid customer info without request date, the JSON is parsed correctly`() {
        val deprecatedValidCachedCustomerInfo by lazy {
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
        val deprecatedValidCachedCustomerInfo by lazy {
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
        val deprecatedValidCachedCustomerInfo by lazy {
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

    // Trusted entitlements: Commented out until ready to be made public
//    @Test
//    fun `given a customer info, the information is cached with a verification result`() {
//        every {
//            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey(appUserID), any())
//        } returns mockEditor
//
//        val info = createCustomerInfo(Responses.validFullPurchaserResponse, null, VerificationResult.VERIFIED)
//        val infoJSONSlot = slot<String>()
//
//        every {
//            mockEditor.putString(any(), capture(infoJSONSlot))
//        } returns mockEditor
//
//        cache.cacheCustomerInfo(appUserID, info)
//
//        val cachedJSON = JSONObject(infoJSONSlot.captured)
//        assertThat(cachedJSON.has("verification_result")).isTrue
//        assertThat(cachedJSON.getString("verification_result")).isEqualTo(VerificationResult.VERIFIED.name)
//    }

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
        mockString(cache.appUserIDCacheKey, "appUserID")
        mockString(cache.legacyAppUserIDCacheKey, "legacyAppUserID")
        mockString(cache.customerInfoCacheKey(appUserID), null)

        cache.clearCachesForAppUserID("appUserID")

        verify { mockEditor.remove(cache.appUserIDCacheKey) }
        verify { mockEditor.remove(cache.legacyAppUserIDCacheKey) }
        verify { mockEditor.remove(cache.customerInfoCacheKey("appUserID")) }
        verify { mockEditor.remove(cache.customerInfoCacheKey("legacyAppUserID")) }
    }

    @Test
    fun `clearing caches clears timestamps`() {
        val date = Date()
        every {
            mockEditor.putLong(cache.customerInfoLastUpdatedCacheKey("appUserID"), date.time)
        } returns mockEditor
        cache.setCustomerInfoCacheTimestamp("appUserID", date)
        cache.setOfferingsCacheTimestampToNow()

        mockString(cache.appUserIDCacheKey, "appUserID")
        mockString(cache.legacyAppUserIDCacheKey, "legacyAppUserID")
        mockString(cache.customerInfoCacheKey(appUserID), null)

        cache.clearCachesForAppUserID("appUserID")
        verify {
            mockEditor.remove(cache.customerInfoLastUpdatedCacheKey("appUserID"))
        }
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue
    }

    @Test
    fun `invalidating offerings caches`() {
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue
        cache.setOfferingsCacheTimestampToNow()
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isFalse
        cache.clearOfferingsCacheTimestamp()
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue
    }

    @Test
    fun `stale if no caches`() {
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue
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
    fun `isOfferingsCacheStale returns true if the cached object is stale`() {
        val offeringsCachedObject = mockk<InMemoryCachedObject<Offerings>>(relaxed = true)
        cache = DeviceCache(mockPrefs, apiKey, offeringsCachedObject)
        cache.cacheOfferings(mockk())
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns Date()
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isFalse

        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MINUTE, -6)
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns cal.time
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue
    }

    @Test
    fun `isOfferingsCacheStale in background returns true if the cached object is stale`() {
        val offeringsCachedObject = mockk<InMemoryCachedObject<Offerings>>(relaxed = true)
        cache = DeviceCache(mockPrefs, apiKey, offeringsCachedObject)
        cache.cacheOfferings(mockk())
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns Date()
        assertThat(cache.isOfferingsCacheStale(appInBackground = true)).isFalse

        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DATE, -2)
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns cal.time
        assertThat(cache.isOfferingsCacheStale(appInBackground = true)).isTrue
    }

    @Test
    fun `caching offerings works`() {
        val storeProduct = mockk<StoreProduct>().also {
            every { it.id } returns "onemonth_freetrial"
        }
        val packageObject = Package(
            "custom",
            PackageType.CUSTOM,
            storeProduct,
            "offering_a"
        )
        val offering = Offering(
            "offering_a",
            "This is the base offering",
            listOf(packageObject)
        )
        val offerings = Offerings(
            offering,
            mapOf(offering.identifier to offering)
        )

        cache.cacheOfferings(offerings)
        assertThat(cache.cachedOfferings).isEqualTo(offerings)
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
                "{\"products\":[{\"id\":\"com.revenuecat.foo_1\",\"entitlements\":[\"pro_1\"]},{\"id\":\"com.revenuecat.foo_2\",\"entitlements\":[\"pro_1\",\"pro_2\"]},{\"id\":\"com.revenuecat.foo_3\",\"entitlements\":[\"pro_2\"]}]}"
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
            mockEditor.putString(productEntitlementMappingCacheKey, "{\"products\":[]}")
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
    fun `getProductEntitlementMapping returns correct product entitlements mapping from cache`() {
        val expectedMappings = createProductEntitlementMapping()
        every {
            mockPrefs.getString(productEntitlementMappingCacheKey, null)
        } returns expectedMappings.toJson().toString()
        assertThat(cache.getProductEntitlementMapping()).isEqualTo(expectedMappings)
    }

    // endregion

    // region order ids cache

    @Test
    fun `getPreviouslySentOrderIdsPerHashToken returns an emptyMap if there's a ClassCastException when calling getString`() {
        every {
            mockPrefs.getString(cache.orderIdsPerTokenCacheKey, any())
        } throws ClassCastException("java.util.Set cannot be cast to java.lang.String")
        val sentTokens = cache.getPreviouslySentOrderIdsPerHashToken()
        assertThat(sentTokens).isEmpty()
    }

    @Test
    fun `getting the tokens not in order ids cache returns all the active tokens that have not been sent`() {
        val cachedTokensAndOrderIds = mapOf("token1" to "order_id", "token2" to "order_id_2")
        mockOrderIdsPerTokenCache(cachedTokensAndOrderIds)

        val activeSub = mockk<StoreTransaction>(relaxed = true).also {
            every { it.type } returns ProductType.SUBS
        }
        val inApp = mockk<StoreTransaction>(relaxed = true).also {
            every { it.type } returns ProductType.INAPP
        }
        val activePurchasesNotInCache: List<StoreTransaction> =
            cache.getActivePurchasesNotInCache(mapOf("token3" to activeSub, "token2" to inApp))
        assertThat(activePurchasesNotInCache.size).isEqualTo(1)
        assertThat(activePurchasesNotInCache[0]).isEqualTo(activeSub)
    }

    @Test
    fun `if all tokens and orders are active, do not remove any`() {
        val expectedSavedCache = mapOf("token1" to "order_id_1", "token2" to "order_id_2")
        mockSavingOrderIdsPerToken(expectedSavedCache)

        val cachedTokensAndOrderIds = mapOf("token1" to "order_id", "token2" to "order_id_2")
        mockOrderIdsPerTokenCache(cachedTokensAndOrderIds)

        cache.cleanInactiveTokens(cachedTokensAndOrderIds.keys)
        verifySavedOrderIdsPerToken(cachedTokensAndOrderIds)
    }

    @Test
    fun `if token is not active anymore, remove it from cache of tokens and order ids`() {
        val expectedSavedCache = mapOf("token3" to "order_id_3")
        mockSavingOrderIdsPerToken(expectedSavedCache)

        val tokensAndOrderIds = mapOf("token1" to "order_id", "token2" to "order_id_2", "token3" to "order_id_3")
        mockOrderIdsPerTokenCache(tokensAndOrderIds)

        cache.cleanInactiveTokens(setOf("token3", "token4"))
        verifySavedOrderIdsPerToken(expectedSavedCache)
    }

    @Test
    fun `token is hashed then added with order id`() {
        val tokensAndOrderIds = mapOf("token1" to "order_id", "token2" to "order_id_2")
        mockOrderIdsPerTokenCache(tokensAndOrderIds)

        val sha1 = "token3".sha1()
        val newMap = tokensAndOrderIds.toMutableMap() + (sha1 to "order_id_3")
        mockSavingOrderIdsPerToken(newMap)

        cache.addSuccessfullyPostedPurchase("token3", "order_id_3")
        verifySavedOrderIdsPerToken(newMap)
    }

    @Test
    fun `getting sent token and order ids works`() {
        val tokensAndOrderIds = mockOrderIdsPerTokenCache(mapOf("token1" to "order_id", "token2" to "order_id_2"))

        val sentTokens = cache.getPreviouslySentOrderIdsPerHashToken()
        assertThat(sentTokens).isEqualTo(tokensAndOrderIds)
    }

    @Test
    fun `getPreviouslySentOrderIdsPerHashToken returns an emptyMap if there's a JSONException when calling getString`() {
        every {
            mockPrefs.getString(cache.orderIdsPerTokenCacheKey, any())
        } throws JSONException("java.util.Set cannot be cast to java.lang.String")
        val sentTokens = cache.getPreviouslySentOrderIdsPerHashToken()
        assertThat(sentTokens).isEmpty()
    }

    @Test
    fun `addSuccessfullyPostedTokenWithOrderId supports saving new order id for existing token`() {
        val token1Hash = "token1".sha1()
        val token2Hash = "token2".sha1()

        val tokensAndOrderIds = mapOf(token1Hash to "order_id", token2Hash to "order_id_2")
        mockOrderIdsPerTokenCache(tokensAndOrderIds)

        val newMap = tokensAndOrderIds.toMutableMap().also { it[token1Hash] = "order_id_3" }
        mockSavingOrderIdsPerToken(newMap)

        cache.addSuccessfullyPostedPurchase("token1", "order_id_3")
        verifySavedOrderIdsPerToken(newMap)
    }

    @Test
    fun `migration of cache of hashed tokens to order ids is skipped if cache of hashed tokens is empty`() {
        mockTokensCache(setOf())
        mockOrderIdsPerTokenCache(mapOf("token1" to "order_id", "token2" to "order_id_2"))

        cache.migrateHashedTokensCacheToCacheWithOrderIds(mapOf())

        verify (exactly = 0) {
            mockEditor.putString(cache.orderIdsPerTokenCacheKey, any())
        }
    }

    @Test
    fun `migration of cache of hashed tokens to order ids saves current order id if purchase is active`() {
        mockTokensCache(setOf("token1", "token2"))
        mockOrderIdsPerTokenCache(mapOf())

        val activeSub = mockk<StoreTransaction>(relaxed = true).also {
            every { it.type } returns ProductType.SUBS
            every { it.orderId } returns "order_id_1"
        }

        cache.migrateHashedTokensCacheToCacheWithOrderIds(mapOf("token1" to activeSub))

        val newMap = mapOf("token1" to "order_id_1", "token2" to "")
        verifySavedOrderIdsPerToken(newMap)
    }

    @Test
    fun `migration of cache of hashed tokens to order ids cleans hashed tokens cache`() {
        mockTokensCache(setOf("token1", "token2"))
        mockOrderIdsPerTokenCache(mapOf())

        val activeSub = mockk<StoreTransaction>(relaxed = true).also {
            every { it.type } returns ProductType.SUBS
            every { it.orderId } returns "order_id_1"
        }

        cache.migrateHashedTokensCacheToCacheWithOrderIds(mapOf("token1" to activeSub))

        verify {
            mockEditor.remove(cache.tokensCacheKey)
        }
    }

    // endregion

    // region helpers

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

    private fun mockTokensCache(tokens: Set<String>) {
        every {
            mockPrefs.getStringSet(cache.tokensCacheKey, any())
        } returns tokens
    }

    private fun mockOrderIdsPerTokenCache(tokensAndOrderIds: Map<String, String>): Map<String, String> {
        every {
            mockPrefs.getString(cache.orderIdsPerTokenCacheKey, any())
        } returns JSONObject(tokensAndOrderIds).toString()
        return tokensAndOrderIds
    }

    private fun mockSavingOrderIdsPerToken(newMap: Map<String, String>) {
        every {
            mockEditor.putString(cache.orderIdsPerTokenCacheKey, JSONObject(newMap).toString())
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs
    }

    private fun verifySavedOrderIdsPerToken(expectedSavedCache: Map<String, String>) {
        verify {
            mockEditor.putString(cache.orderIdsPerTokenCacheKey, JSONObject(expectedSavedCache).toString())
        }
    }

    // endregion
}
