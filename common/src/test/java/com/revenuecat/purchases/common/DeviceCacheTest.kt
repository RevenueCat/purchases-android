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
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.PURCHASER_INFO_SCHEMA_VERSION
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DeviceCacheTest {

    private val validCachedPurchaserInfo by lazy {
        JSONObject(Responses.validFullPurchaserResponse).apply {
            put("schema_version", PURCHASER_INFO_SCHEMA_VERSION)
        }.toString()
    }

    private val oldCachedPurchaserInfo =
        "{'schema_version': 0, 'request_date': '2018-10-19T02:40:36Z', 'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z', 'purchase_date': '2018-10-26T23:17:53Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}"

    private lateinit var cache: DeviceCache
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val apiKey = "api_key"
    private val appUserID = "app_user_id"
    private val legacyAppUserID = "app_user_id"

    private val slotForPutLong = slot<Long>()

    @Before
    fun setup() {
        mockPrefs = mockk()
        mockEditor = mockk()

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

        cache = DeviceCache(mockPrefs, apiKey)
    }

    @Test
    fun `cache is created properly`() {
        assertThat(cache).`as`("cache is not null").isNotNull
    }

    @Test
    fun `given no cached info, cached purchased info is null`() {
        mockString(cache.purchaserInfoCacheKey(appUserID), null)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a purchaser info, the key in the cache is correct`() {
        mockString(cache.purchaserInfoCacheKey(appUserID), Responses.validFullPurchaserResponse.toString())
        cache.getCachedPurchaserInfo(appUserID)
        verify {
            mockPrefs.getString(cache.purchaserInfoCacheKey(appUserID), isNull())
        }
    }

    @Test
    fun `given a valid purchaser info, the JSON is parsed correctly`() {
        mockString(cache.purchaserInfoCacheKey(appUserID), validCachedPurchaserInfo)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
    }

    @Test
    fun `given a invalid purchaser info, the information is null`() {
        mockString(cache.purchaserInfoCacheKey(appUserID), "not json")
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a purchaser info, the information is cached`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = jsonObject.buildPurchaserInfo()

        cache.cachePurchaserInfo(appUserID, info)
        assertThat(slotForPutLong.captured).isNotNull()
        verifyAll {
            mockEditor.putString(cache.purchaserInfoCacheKey(appUserID), any())
            mockEditor.putLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), slotForPutLong.captured)
            mockEditor.apply()
        }
    }

    @Test
    fun `given a purchaser info, the information is cached with a schema version`() {
        every {
            mockEditor.putLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), any())
        } returns mockEditor

        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = jsonObject.buildPurchaserInfo()
        val infoJSONSlot = slot<String>()

        every {
            mockEditor.putString(any(), capture(infoJSONSlot))
        } returns mockEditor
        cache.cachePurchaserInfo(appUserID, info)

        val cachedJSON = JSONObject(infoJSONSlot.captured)
        assertThat(cachedJSON.has("schema_version"))
        assertThat(cachedJSON.getInt("schema_version")).isEqualTo(PURCHASER_INFO_SCHEMA_VERSION)
    }

    @Test
    fun `given an older version of purchaser info, nothing is returned`() {
        mockString(cache.purchaserInfoCacheKey(appUserID), oldCachedPurchaserInfo)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a valid version purchaser info, it is returned`() {
        mockString(cache.purchaserInfoCacheKey(appUserID), validCachedPurchaserInfo)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull()
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
        val activeSub = mockk<PurchaseDetails>(relaxed = true).also {
            every { it.type } returns ProductType.SUBS
        }
        val inApp = mockk<PurchaseDetails>(relaxed = true).also {
            every { it.type } returns ProductType.INAPP
        }
        val activePurchasesNotInCache =
            cache.getActivePurchasesNotInCache(mapOf("hash1" to activeSub, "hash2" to inApp))
        assertThat(activePurchasesNotInCache).contains(activeSub)
    }

    @Test
    fun `invalidating purchaser info caches`() {
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = false)).isTrue()
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = false)).isFalse()
        cache.clearPurchaserInfoCacheTimestamp(appUserID)
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), 0L)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = false)).isTrue()
    }

    @Test
    fun `clearing caches clears all user ID data`() {
        every {
            mockEditor.putLong(cache.purchaserInfoLastUpdatedCacheKey("appUserID"), capture(slot()))
        } returns mockEditor
        mockString(cache.appUserIDCacheKey, "appUserID")
        mockString(cache.legacyAppUserIDCacheKey, "legacyAppUserID")
        mockString(cache.purchaserInfoCacheKey(appUserID), null)

        cache.clearCachesForAppUserID("appUserID")

        verify { mockEditor.remove(cache.appUserIDCacheKey) }
        verify { mockEditor.remove(cache.legacyAppUserIDCacheKey) }
        verify { mockEditor.remove(cache.purchaserInfoCacheKey("appUserID")) }
        verify { mockEditor.remove(cache.purchaserInfoCacheKey("legacyAppUserID")) }
    }

    @Test
    fun `clearing caches clears timestamps`() {
        val date = Date()
        every {
            mockEditor.putLong(cache.purchaserInfoLastUpdatedCacheKey("appUserID"), date.time)
        } returns mockEditor
        cache.setPurchaserInfoCacheTimestamp("appUserID", date)
        cache.setOfferingsCacheTimestampToNow()

        mockString(cache.appUserIDCacheKey, "appUserID")
        mockString(cache.legacyAppUserIDCacheKey, "legacyAppUserID")
        mockString(cache.purchaserInfoCacheKey(appUserID), null)

        cache.clearCachesForAppUserID("appUserID")
        verify {
            mockEditor.remove(cache.purchaserInfoLastUpdatedCacheKey("appUserID"))
        }
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue()
    }

    @Test
    fun `invalidating offerings caches`() {
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue()
        cache.setOfferingsCacheTimestampToNow()
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isFalse()
        cache.clearOfferingsCacheTimestamp()
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue()
    }

    @Test
    fun `stale if no caches`() {
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue()
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), 0L)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = false)).isTrue()
    }

    @Test
    fun `isPurchaserInfoCacheStale returns true if the cached object is stale`() {
        cache.cachePurchaserInfo(appUserID, mockk(relaxed = true))
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = false)).isTrue()
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = false)).isFalse()
    }

    @Test
    fun `isPurchaserInfoCacheStale in background returns true if the cached object is stale`() {
        cache.cachePurchaserInfo(appUserID, mockk(relaxed = true))
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), Date(0).time)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = true)).isTrue()
        mockLong(cache.purchaserInfoLastUpdatedCacheKey(appUserID), Date().time)
        assertThat(cache.isPurchaserInfoCacheStale(appUserID, appInBackground = true)).isFalse()
    }

    @Test
    fun `isOfferingsCacheStale returns true if the cached object is stale`() {
        val offeringsCachedObject = mockk<InMemoryCachedObject<Offerings>>(relaxed = true)
        cache = DeviceCache(mockPrefs, apiKey, offeringsCachedObject)
        cache.cacheOfferings(mockk())
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns Date()
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isFalse()

        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.MINUTE, -6)
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns cal.time
        assertThat(cache.isOfferingsCacheStale(appInBackground = false)).isTrue()
    }

    @Test
    fun `isOfferingsCacheStale in background returns true if the cached object is stale`() {
        val offeringsCachedObject = mockk<InMemoryCachedObject<Offerings>>(relaxed = true)
        cache = DeviceCache(mockPrefs, apiKey, offeringsCachedObject)
        cache.cacheOfferings(mockk())
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns Date()
        assertThat(cache.isOfferingsCacheStale(appInBackground = true)).isFalse()

        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DATE, -2)
        every {
            offeringsCachedObject.lastUpdatedAt
        } returns cal.time
        assertThat(cache.isOfferingsCacheStale(appInBackground = true)).isTrue()
    }

    @Test
    fun `caching offerings works`() {
        val productDetails = mockk<ProductDetails>().also {
            every { it.sku } returns "onemonth_freetrial"
        }
        val packageObject = Package(
            "custom",
            PackageType.CUSTOM,
            productDetails,
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
    fun `timestamp is set when caching purchaser info`() {
        cache.cachePurchaserInfo("waldo", mockk(relaxed = true))
        assertThat(slotForPutLong.captured).isNotNull()
    }

    @Test
    fun `clearing purchaser info caches clears the shared preferences`() {
        cache.cachePurchaserInfo(appUserID, mockk(relaxed = true))
        assertThat(slotForPutLong.captured).isNotNull()

        cache.clearPurchaserInfoCache(appUserID)
        verify { mockEditor.remove(cache.purchaserInfoCacheKey(appUserID)) }
    }

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
