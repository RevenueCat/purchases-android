package com.revenuecat.purchases

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PurchasesFactoryTest {

    private val contextMock = InstrumentationRegistry.getInstrumentation().targetContext
    private val applicationMock = contextMock.applicationContext as Application
    private val apiKeyValidatorMock = mockk<APIKeyValidator>()

    private lateinit var purchasesFactory: PurchasesFactory

    @Before
    fun setup() {
        purchasesFactory = PurchasesFactory(isDebugBuild = { true }, apiKeyValidatorMock)

        every { apiKeyValidatorMock.validateAndLog("fakeApiKey", Store.PLAY_STORE) } just runs
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `creating purchase checks context has INTERNET permission`() {
        val configuration = createConfiguration()
        shadowOf(applicationMock).denyPermissions(Manifest.permission.INTERNET)
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            purchasesFactory.validateConfiguration(configuration)
        }.withMessage("Purchases requires INTERNET permission.")
    }

    @Test
    fun `creating purchase checks api key is not empty`() {
        val configuration = createConfiguration(testApiKey = "")
        shadowOf(applicationMock).grantPermissions(Manifest.permission.INTERNET)
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            purchasesFactory.validateConfiguration(configuration)
        }.withMessage("API key must be set. Get this from the RevenueCat web app")
    }

    @Test
    fun `creating purchase checks context application context is an application`() {
        val applicationMock = mockk<Context>()
        val contextMock = mockk<Context>().apply {
            every { applicationContext } returns applicationMock
        }
        val nonApplicationContextMock = mockk<Context>()
        every {
            applicationMock.applicationContext
        } returns nonApplicationContextMock
        every {
            applicationMock.checkCallingOrSelfPermission(Manifest.permission.INTERNET)
        } returns PackageManager.PERMISSION_GRANTED
        val configuration = createConfiguration(context = contextMock)

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            purchasesFactory.validateConfiguration(configuration)
        }.withMessage("Needs an application context.")
    }

    @Test
    fun `creating purchase validates api key is valid`() {
        val configuration = createConfiguration()
        shadowOf(applicationMock).grantPermissions(Manifest.permission.INTERNET)
        purchasesFactory.validateConfiguration(configuration)
        verify(exactly = 1) { apiKeyValidatorMock.validateAndLog("fakeApiKey", Store.PLAY_STORE) }
    }

    private fun createConfiguration(
        context: Context = contextMock,
        testApiKey: String = "fakeApiKey",
    ): PurchasesConfiguration {
        return PurchasesConfiguration.Builder(context, testApiKey)
            .appUserID("appUserID")
            .store(Store.PLAY_STORE)
            .build()
    }
}
