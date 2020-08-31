package com.revenuecat.purchases.subscriberattributes

import android.app.Application
import android.content.Context
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesRepairableException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class AttributionFetcherTests {

    private lateinit var underTest: AttributionFetcher

    @Before
    fun setup() {
        underTest = AttributionFetcher()
    }

    @Test
    fun `getDeviceIdentifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345",
            expectedAndroidID = "androidid"
        )

        val (advertisingID, androidID) = underTest.getDeviceIdentifiers(mockContext)

        Assertions.assertThat(advertisingID).isNotNull()
        Assertions.assertThat(advertisingID).isEqualTo("12345")

        Assertions.assertThat(androidID).isEqualTo("androidid")
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

        val (advertisingID, androidID) = underTest.getDeviceIdentifiers(mockContext)

        Assertions.assertThat(advertisingID).isNull()

        Assertions.assertThat(androidID).isEqualTo("androidid")
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

        val (advertisingID, androidID) = underTest.getDeviceIdentifiers(mockContext)

        Assertions.assertThat(advertisingID).isNull()

        Assertions.assertThat(androidID).isEqualTo("androidid")
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

        val (advertisingID, androidID) = underTest.getDeviceIdentifiers(mockContext)

        Assertions.assertThat(advertisingID).isNull()

        Assertions.assertThat(androidID).isEqualTo("androidid")
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
