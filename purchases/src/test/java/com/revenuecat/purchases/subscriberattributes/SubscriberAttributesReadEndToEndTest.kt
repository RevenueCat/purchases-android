@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.subscriberattributes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerAttribute
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * From a CustomerInfo arriving at [CustomerInfoUpdateHandler] to what `Purchases.attributesForCurrentUser`
 * returns, through the real manager, cache and `SharedPreferences`.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberAttributesReadEndToEndTest {

    private val appUserID = "waldo"

    private lateinit var deviceCache: DeviceCache
    private lateinit var attributesManager: SubscriberAttributesManager
    private lateinit var handler: CustomerInfoUpdateHandler

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("subscriber_attributes_read_e2e_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        deviceCache = DeviceCache(preferences, apiKey = "api_key")
        attributesManager = SubscriberAttributesManager(
            SubscriberAttributesCache(deviceCache),
            backend = mockk(),
            deviceIdentifiersFetcher = mockk(),
            automaticDeviceIdentifierCollectionEnabled = false,
        )
        handler = CustomerInfoUpdateHandler(
            deviceCache,
            attributesManager,
            identityManager = mockk<IdentityManager>().apply { every { currentAppUserID } returns appUserID },
            offlineEntitlementsManager = mockk(),
            appConfig = mockk(),
            diagnosticsTracker = null,
        )
    }

    @Test
    fun `attributes from CustomerInfo are readable, without deleted ones`() {
        handler.cacheAndNotifyListeners(
            customerInfoWithSubscriberAttributes(
                remoteAttributesJSON("band" to remoteEntry("Rush"), "\$email" to remoteEntry(null)),
            ),
            appUserID,
        )

        assertThat(readable().map { it.key }).containsExactly("band")
        assertThat(readable().single().lastUpdatedDate).isEqualTo(remoteSetTime)
    }

    @Test
    fun `a local attribute set after the fetch is readable alongside the server's`() {
        handler.cacheAndNotifyListeners(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )

        attributesManager.setAttributes(mapOf("song" to "YYZ"), appUserID)

        assertThat(readable().associate { it.key to it.value }).isEqualTo(mapOf("band" to "Rush", "song" to "YYZ"))
    }

    @Test
    fun `a local deletion hides the attribute until the server agrees`() {
        handler.cacheAndNotifyListeners(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )

        attributesManager.setAttributes(mapOf("band" to null), appUserID)
        assertThat(readable()).isEmpty()

        // a refetch from before the deletion reached the server must not bring it back
        handler.cacheAndNotifyListeners(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )
        assertThat(readable()).isEmpty()
    }

    @Test
    fun `an attribute deleted on the server disappears on the next fetch`() {
        handler.cacheAndNotifyListeners(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )

        handler.cacheAndNotifyListeners(customerInfoWithSubscriberAttributes(remoteAttributesJSON()), appUserID)

        assertThat(readable()).isEmpty()
    }

    @Test
    fun `the customer info is still cached when its attributes are malformed`() {
        val info = customerInfoWithSubscriberAttributes("not an object")

        handler.cacheAndNotifyListeners(info, appUserID)

        assertThat(deviceCache.getCachedCustomerInfo(appUserID)?.originalAppUserId).isEqualTo(info.originalAppUserId)
        assertThat(readable()).isEmpty()
    }

    @Test
    fun `the customer info is still cached when the attribute cache is corrupt`() {
        deviceCache.putString(
            SubscriberAttributesCache(deviceCache).subscriberAttributesCacheKey,
            """{"attributes": {"$appUserID": []}}""",
        )
        val info = customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush")))

        handler.cacheAndNotifyListeners(info, appUserID)

        assertThat(deviceCache.getCachedCustomerInfo(appUserID)?.originalAppUserId).isEqualTo(info.originalAppUserId)
        assertThat(readable()).isEmpty()
    }

    private fun readable(): List<CustomerAttribute> =
        attributesManager.getStoredAttributes(appUserID).toCustomerAttributes()
}
