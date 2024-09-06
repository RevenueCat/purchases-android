package com.revenuecat.purchases.google.attribution

import android.app.Application
import android.provider.Settings
import android.util.Log
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class GoogleDeviceIdentifiersFetcherTests {

    private lateinit var underTest: GoogleDeviceIdentifiersFetcher

    @Before
    fun setup() {
        underTest = GoogleDeviceIdentifiersFetcher(SyncDispatcher())
        mockkStatic(Log::class)
        every {
            Log.e(any(), any())
        } returns 0
        every {
            Log.w(any(), any<String>())
        } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun getDeviceIdentifiers() {
        val mockContext = mockk<Application>(relaxed = true)

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue()
    }

    /**
     * Assert that [identifiers] does not contain the [Settings.Secure.ANDROID_ID].
     */
    private fun assertContainsNoAndroidId(identifiers: Map<String, String>) {
        // We used to use this key for the ANDROID_ID, both in the identifiers map and when sending it to the backend.
        assertThat(identifiers.containsKey("\$androidId")).isFalse()
        // This value is set by mockAdvertisingInfo(). 
        assertThat(identifiers.containsValue("androidid")).isFalse()
    }
}
