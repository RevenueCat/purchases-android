package com.revenuecat.purchases.amazon.attribution

import android.app.Application
import android.content.ContentResolver
import android.provider.Settings
import android.util.Log
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class AmazonDeviceIdentifiersFetcherTests {

    private lateinit var underTest: AmazonDeviceIdentifiersFetcher
    private val mockApplication = mockk<Application>()
    private val mockContentResolver = mockk<ContentResolver>()

    @Before
    fun setup() {
        underTest = AmazonDeviceIdentifiersFetcher()
        every {
            mockApplication.contentResolver
        } returns mockContentResolver

        mockkStatic(Settings.Secure::class)
        mockkStatic(Log::class)
        every {
            Log.e(any(), any())
        } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun `getDeviceIdentifiers returns empty advertising ID if ad tracking is limited`() {
        var completionCalled = false
        var receivedDeviceIdentifiers = emptyMap<String, String>()
        every {
            Settings.Secure.getInt(mockContentResolver, "limit_ad_tracking")
        } returns 1
        underTest.getDeviceIdentifiers(mockApplication) { deviceIdentifiers ->
            completionCalled = true
            receivedDeviceIdentifiers = deviceIdentifiers
        }

        assertThat(completionCalled).isTrue()
        assertThat(receivedDeviceIdentifiers.size).isEqualTo(2)
        assertAttributionProperties(receivedDeviceIdentifiers)
    }

    @Test
    fun `getDeviceIdentifiers returns advertising ID if ad tracking is not limited`() {
        var completionCalled = false
        var receivedDeviceIdentifiers = emptyMap<String, String>()
        val expectedAmazonAdID = "expectedAmazonAdID"
        every {
            Settings.Secure.getInt(mockContentResolver, "limit_ad_tracking")
        } returns 0
        every {
            Settings.Secure.getString(mockContentResolver, "advertising_id")
        } returns expectedAmazonAdID
        underTest.getDeviceIdentifiers(mockApplication) { deviceIdentifiers ->
            completionCalled = true
            receivedDeviceIdentifiers = deviceIdentifiers
        }

        assertThat(completionCalled).isTrue()
        assertThat(receivedDeviceIdentifiers.size).isEqualTo(3)
        assertThat(receivedDeviceIdentifiers[SubscriberAttributeKey.DeviceIdentifiers.AmazonAdID.backendKey])
            .isEqualTo(expectedAmazonAdID)
        assertAttributionProperties(receivedDeviceIdentifiers)
    }

    @Test
    fun `getDeviceIdentifiers catches SettingNotFoundException when getting limit_ad_tracking setting`() {
        var completionCalled = false
        var receivedDeviceIdentifiers = emptyMap<String, String>()
        every {
            Settings.Secure.getInt(mockContentResolver, "limit_ad_tracking")
        } throws Settings.SettingNotFoundException("SettingNotFoundException")
        every {
            Settings.Secure.getString(mockContentResolver, "advertising_id")
        } returns "expectedAmazonAdID"
        underTest.getDeviceIdentifiers(mockApplication) { deviceIdentifiers ->
            completionCalled = true
            receivedDeviceIdentifiers = deviceIdentifiers
        }

        assertThat(completionCalled).isTrue()
        assertThat(receivedDeviceIdentifiers.size).isEqualTo(2)
        assertAttributionProperties(receivedDeviceIdentifiers)
    }

    @Test
    fun `getDeviceIdentifiers catches SettingNotFoundException when getting advertising_id setting`() {
        var completionCalled = false
        var receivedDeviceIdentifiers = emptyMap<String, String>()
        every {
            Settings.Secure.getInt(mockContentResolver, "limit_ad_tracking")
        } returns 1
        every {
            Settings.Secure.getInt(mockContentResolver, "advertising_id")
        } throws Settings.SettingNotFoundException("SettingNotFoundException")

        underTest.getDeviceIdentifiers(mockApplication) { deviceIdentifiers ->
            completionCalled = true
            receivedDeviceIdentifiers = deviceIdentifiers
        }

        assertThat(completionCalled).isTrue()
        assertThat(receivedDeviceIdentifiers.size).isEqualTo(2)
        assertAttributionProperties(receivedDeviceIdentifiers)
    }

    private fun assertAttributionProperties(identifiers: Map<String, String>) {
        assertThat(identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]).isEqualTo("true")
        assertThat(identifiers[SubscriberAttributeKey.DeviceIdentifiers.UserAgent.backendKey]).isEqualTo("true")
    }
}
