package com.revenuecat.purchases

import android.content.SharedPreferences
import android.support.test.runner.AndroidJUnit4
import com.revenuecat.purchases.PurchaserInfoTest.Companion.validFullPurchaserResponse

import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DeviceCacheTest {

    private lateinit var cache: DeviceCache
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val apiKey = "api_key"
    private val appUserID = "app_user_id"
    private val userIDCacheKey = "com.revenuecat.purchases.$apiKey"
    private val purchaserInfoCacheKey = "$userIDCacheKey.$appUserID"

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
            mockPrefs.edit()
        } returns mockEditor

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
        mockString(purchaserInfoCacheKey, validFullPurchaserResponse)
        cache.getCachedPurchaserInfo(appUserID)
        verify {
            mockPrefs.getString(eq(purchaserInfoCacheKey), isNull())
        }
    }

    @Test
    fun `given a valid purchaser info, the JSON is parsed correcly`() {
        mockString(purchaserInfoCacheKey, validFullPurchaserResponse)
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
        every {
            mockEditor.apply()
        } just runs

        val jsonObject = JSONObject(validFullPurchaserResponse)
        val info = PurchaserInfo.Factory.build(jsonObject)

        cache.cachePurchaserInfo(appUserID, info)
        verifyAll {
            mockEditor.putString(eq(purchaserInfoCacheKey), any())
            mockEditor.apply()
        }
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
        val appUserID = cache.getCachedAppUserID()
        assertThat(appUserID).`as`("appUserID is the same as the cached appUserID").isEqualTo(appUserID)
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

    private fun mockString(key: String, value: String?) {
        every {
            mockPrefs.getString(eq(key), isNull())
        } returns value
    }
}
