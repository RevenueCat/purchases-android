package com.revenuecat.purchases.subscriberattributes

import android.app.Application
import android.content.Context
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.revenuecat.purchases.common.Dispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class AttributionFetcherTests {

    private lateinit var underTest: AttributionFetcher

    @Before
    fun setup() {
        val mockDispatcher = mockk<Dispatcher>()
        every {
            mockDispatcher.executeOnBackground(captureLambda())
        } answers {
            lambda<() -> Unit>().captured.also {
                it.invoke()
            }
        }
        underTest = AttributionFetcher(mockDispatcher)
    }

    @Test
    fun `getDeviceIdentifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid"
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { advertisingID, androidID ->
            completionCalled = true
            assertThat(advertisingID).isNotNull()
            assertThat(advertisingID).isEqualTo("12345")

            assertThat(androidID).isEqualTo("androidid")
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
            gpsException = GooglePlayServicesRepairableException(1, "error", null)
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { advertisingID, androidID ->
            completionCalled = true

            assertThat(advertisingID).isNull()
            assertThat(androidID).isEqualTo("androidid")
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
            gpsException = GooglePlayServicesRepairableException(1, "error", null)
        )

        var completionCalled = false
        underTest.getDeviceIdentifiers(mockContext) { advertisingID, androidID ->
            completionCalled = true

            assertThat(advertisingID).isNull()
            assertThat(androidID).isEqualTo("androidid")
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
        underTest.getDeviceIdentifiers(mockContext) { advertisingID, androidID ->
            completionCalled = true

            assertThat(advertisingID).isNull()
            assertThat(androidID).isEqualTo("androidid")
        }

        assertThat(completionCalled).isTrue()
    }

    private fun mockAdvertisingInfo(
        mockContext: Context,
        expectedAdID: String?,
        expectedAndroidID: String?,
        expectedIsLimitAdTrackingEnabled: Boolean = false,
        gpsException: Exception? = null
    ) {
        val mockAdInfo = mockk<AdvertisingIdClient.Info>().apply {
            every { isLimitAdTrackingEnabled } returns expectedIsLimitAdTrackingEnabled
            every { id } returns expectedAdID
        }
        mockkStatic(AdvertisingIdClient::class)
        if (gpsException == null) {
            every {
                AdvertisingIdClient.getAdvertisingIdInfo(mockContext)
            } returns mockAdInfo
        } else {
            every {
                AdvertisingIdClient.getAdvertisingIdInfo(mockContext)
            } throws gpsException
        }

        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(mockContext.contentResolver, Settings.Secure.ANDROID_ID)
        } returns expectedAndroidID
    }
}
