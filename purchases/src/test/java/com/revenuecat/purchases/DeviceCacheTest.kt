//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClient.SkuType.SUBS
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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DeviceCacheTest {

    private val validCachedPurchaserInfo by lazy {
        JSONObject(Responses.validFullPurchaserResponse).apply {
            put("schema_version", PurchaserInfo.SCHEMA_VERSION)
        }.toString()
    }

    private val oldCachedPurchaserInfo =
        "{'schema_version': 0, 'request_date': '2018-10-19T02:40:36Z', 'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z', 'purchase_date': '2018-10-26T23:17:53Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}"

    private lateinit var cache: DeviceCache
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val apiKey = "api_key"
    private val appUserID = "app_user_id"
    private val userIDCacheKey = "com.revenuecat.purchases.$apiKey"
    private val purchaserInfoCacheKey = "$userIDCacheKey.$appUserID"
    private val tokensCacheKey = "com.revenuecat.purchases.$apiKey.tokens"

    @Before
    fun setup() {
        mockPrefs = mockk()
        mockEditor = mockk()

        every {
            mockEditor.putString(any(), any())
        } returns mockEditor
        every {
            mockEditor.putString(any(), any())
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
        mockString(purchaserInfoCacheKey, null)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a purchaser info, the key in the cache is correct`() {
        mockString(purchaserInfoCacheKey, Responses.validFullPurchaserResponse)
        cache.getCachedPurchaserInfo(appUserID)
        verify {
            mockPrefs.getString(eq(purchaserInfoCacheKey), isNull())
        }
    }

    @Test
    fun `given a valid purchaser info, the JSON is parsed correctly`() {
        mockString(purchaserInfoCacheKey, validCachedPurchaserInfo)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull
    }

    @Test
    fun `given a invalid purchaser info, the information is null`() {
        mockString(purchaserInfoCacheKey, "not json")
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    @Throws(JSONException::class)
    fun `given a purchaser info, the information is cached`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = jsonObject.buildPurchaserInfo()

        cache.cachePurchaserInfo(appUserID, info)
        verifyAll {
            mockEditor.putString(eq(purchaserInfoCacheKey), any())
            mockEditor.apply()
        }
    }

    @Test
    fun `given a purchaser info, the information is cached with a schema version`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = jsonObject.buildPurchaserInfo()
        val infoJSONSlot = slot<String>()

        every {
            mockEditor.putString(any(), capture(infoJSONSlot))
        } returns mockEditor
        cache.cachePurchaserInfo(appUserID, info)

        val cachedJSON = JSONObject(infoJSONSlot.captured)
        assertThat(cachedJSON.has("schema_version"))
        assertThat(cachedJSON.getInt("schema_version")).isEqualTo(PurchaserInfo.SCHEMA_VERSION)
    }

    @Test
    fun `given an older version of purchaser info, nothing is returned`() {
        mockString(purchaserInfoCacheKey, oldCachedPurchaserInfo)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is null").isNull()
    }

    @Test
    fun `given a valid version purchaser info, it is returned`() {
        mockString(purchaserInfoCacheKey, validCachedPurchaserInfo)
        val info = cache.getCachedPurchaserInfo(appUserID)
        assertThat(info).`as`("info is not null").isNotNull()
    }

    @Test
    fun `given a non cached appuserid, the cached appuserid is null`() {
        mockString(userIDCacheKey, null)
        val appUserID = cache.getCachedAppUserID()
        assertThat(appUserID).`as`("appUserID is null").isNull()
    }

    @Test
    fun `given a non cached appuserid, the cached appuserid is returned`() {
        mockString(userIDCacheKey, appUserID)
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
            mockEditor.putString(eq(userIDCacheKey), any())
            mockEditor.apply()
        }
    }

    @Test
    fun `getting sent tokens works`() {
        val tokens = setOf("token1", "token2")
        every {
            mockPrefs.getStringSet(tokensCacheKey, any())
        } returns tokens
        val sentTokens = cache.getPreviouslySentHashedTokens()
        assertThat(sentTokens).isEqualTo(tokens)
    }

    @Test
    fun `token is hashed then added`() {
        every {
            mockPrefs.getStringSet(tokensCacheKey, any())
        } returns setOf("token1", "token2")
        val sha1 = "token3".sha1()
        every {
            mockEditor.putStringSet(tokensCacheKey, setOf("token1", "token2", sha1))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs

        cache.addSuccessfullyPostedToken("token3")
        verify {
            mockEditor.putStringSet(tokensCacheKey, setOf("token1", "token2", sha1))
        }
    }

    @Test
    fun `if token is not active anymore, remove it from database`() {
        every {
            mockEditor.putStringSet(tokensCacheKey, setOf("token3"))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs
        every {
            mockPrefs.getStringSet(tokensCacheKey, any())
        } returns setOf("token1", "token2", "token3")
        cache.cleanPreviouslySentTokens(
            setOf("token3"),
            setOf("token4")
        )
        verify {
            mockEditor.putStringSet(tokensCacheKey, setOf("token3"))
        }
    }

    @Test
    fun `if all tokens are active, do not remove any`() {
        every {
            mockEditor.putStringSet(tokensCacheKey, setOf("token1", "token2"))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just runs
        every {
            mockPrefs.getStringSet(tokensCacheKey, any())
        } returns setOf("token1", "token2")
        cache.cleanPreviouslySentTokens(
            setOf("token1"),
            setOf("token2")
        )
        verify {
            mockEditor.putStringSet(tokensCacheKey, setOf("token1", "token2"))
        }
    }

    @Test
    fun `getting the tokens not in cache returns all the active tokens that have not been sent`() {
        every {
            mockPrefs.getStringSet(tokensCacheKey, any())
        } returns setOf("token1", "hash2", "token3")
        val activeSub = PurchaseWrapper(mockk(relaxed = true), SUBS, null)
        val activePurchasesNotInCache =
            cache.getActivePurchasesNotInCache(
                mapOf("hash1" to activeSub),
                mapOf("hash2" to PurchaseWrapper(mockk(relaxed = true), INAPP, null)))
        assertThat(activePurchasesNotInCache).contains(activeSub)
    }

    private fun mockString(key: String, value: String?) {
        every {
            mockPrefs.getString(eq(key), isNull())
        } returns value
    }
}
