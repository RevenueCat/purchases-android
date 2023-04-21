package com.revenuecat.purchases.backend_integration_tests

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BackendIntegrationTest {

    companion object {
        private val TIMEOUT = 5.seconds
    }

    lateinit var appConfig: AppConfig
    lateinit var dispatcher: Dispatcher
    lateinit var diagnosticsDispatcher: Dispatcher
    lateinit var sharedPreferences: SharedPreferences
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    lateinit var eTagManager: ETagManager
    lateinit var signingManager: SigningManager
    lateinit var httpClient: HTTPClient
    lateinit var backendHelper: BackendHelper

    lateinit var backend: Backend

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns URL("https://api.revenuecat.com")
            every { store } returns Store.PLAY_STORE
            every { platformInfo } returns PlatformInfo("test-flavor", version = null)
            every { languageTag } returns "en-US"
            every { versionName } returns "test-version-name"
            every { packageName } returns "com.revenuecat.purchases.backend_tests"
            every { finishTransactions } returns true
        }
        dispatcher = Dispatcher(Executors.newSingleThreadScheduledExecutor())
        diagnosticsDispatcher = Dispatcher(Executors.newSingleThreadScheduledExecutor())
        sharedPreferencesEditor = mockk<SharedPreferences.Editor>().apply {
            every { putString(any(), any()) } returns this
            every { apply() } just Runs
        }
        sharedPreferences = mockk<SharedPreferences>().apply {
            every { getString(any(), any()) } answers { secondArg() as String? }
            every { edit() } returns sharedPreferencesEditor
        }
        eTagManager = ETagManager(sharedPreferences)
        signingManager = SigningManager(SignatureVerificationMode.Disabled)
        httpClient = HTTPClient(appConfig, eTagManager, diagnosticsTrackerIfEnabled = null, signingManager)
        backendHelper = BackendHelper(Constants.apiKey, dispatcher, appConfig, httpClient)
        backend = Backend(appConfig, dispatcher, diagnosticsDispatcher, httpClient, backendHelper)
    }

    @Test
    fun canPerformProductEntitlementMappingBackendRequest() {
        ensureBlockFinishes { latch ->
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    assertThat(productEntitlementMapping.mappings).isNotEmpty
                    latch.countDown()
                },
                onErrorHandler = {
                    fail("Request should succeed")
                }
            )
        }
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString("/v1${Endpoint.GetProductEntitlementMapping.getPath()}", any())
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
    }

    private fun ensureBlockFinishes(block: (CountDownLatch) -> Unit) {
        val latch = CountDownLatch(1)
        block(latch)
        latch.await(TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(latch.count).isEqualTo(0)
    }
}
