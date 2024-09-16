package com.revenuecat.purchases.google.attribution

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesRepairableException
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
import java.io.IOException
import java.util.concurrent.TimeoutException

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
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid"
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true
            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNotNull()
            assertThat(advertisingID).isEqualTo("12345")

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")

            assertContainsDeviceVersion(identifiers)
        }

        assertThat(completionCalled).isTrue()
    }

    @Test
    fun `GooglePlayServicesNotAvailableException when calling getDeviceIdentifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid",
            expectedException = GooglePlayServicesRepairableException(1, "error", mockk())
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")

            assertContainsDeviceVersion(identifiers)
        }

        assertThat(completionCalled).isTrue()
    }

    @Test
    fun `GooglePlayServicesRepairableException when calling getDeviceIdentifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid",
            expectedException = GooglePlayServicesRepairableException(1, "error", mockk())
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue()
    }

    @Test
    fun `getDeviceIdentifiers when ad tracking is limited`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid",
            expectedIsLimitAdTrackingEnabled = true
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue()
    }

    @Test
    fun `getDeviceIdentifiers when returns all zeros`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "00000000-0000-0000-0000-000000000000",
            expectedAndroidID = "androidid"
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue
    }

    @Test
    fun `IOException when getting device identifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid",
            expectedException = IOException(TimeoutException())
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue()
    }

    @Test
    fun `NullPointerException when getting device identifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid",
            expectedException = NullPointerException()
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            assertContainsNoAndroidId(identifiers)

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue()
    }

    @Test
    fun `TimeoutException when getting device identifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid",
            expectedException = TimeoutException()
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { identifiers ->
            completionCalled = true

            val advertisingID = identifiers[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
            assertThat(advertisingID).isNull()

            

            val ip = identifiers[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
            assertThat(ip).isEqualTo("true")
        }

        assertThat(completionCalled).isTrue()
    }

    private fun mockAdvertisingInfo(
        mockContext: Context,
        expectedAdID: String?,
        expectedAndroidID: String?,
        expectedIsLimitAdTrackingEnabled: Boolean = false,
        expectedException: Exception? = null
    ) {
        val mockAdInfo = mockk<AdvertisingIdClient.Info>().apply {
            every { isLimitAdTrackingEnabled } returns expectedIsLimitAdTrackingEnabled
            every { id } returns expectedAdID
        }
        mockkStatic(AdvertisingIdClient::class)
        if (expectedException == null) {
            every {
                AdvertisingIdClient.getAdvertisingIdInfo(mockContext)
            } returns mockAdInfo
        } else {
            every {
                AdvertisingIdClient.getAdvertisingIdInfo(mockContext)
            } throws expectedException
        }

        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(mockContext.contentResolver, Settings.Secure.ANDROID_ID)
        } returns expectedAndroidID
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

    private fun assertContainsDeviceVersion(identifiers: Map<String, String>) {
        assertThat(identifiers[SubscriberAttributeKey.DeviceIdentifiers.DeviceVersion.backendKey]).isEqualTo("true")
    }
}
