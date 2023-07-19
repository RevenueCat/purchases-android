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
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

// To run these tests in Android Studio, you need to remove the test exclusion in the
// common module build.gradle and change the API KEY in Constants.kt
@RunWith(AndroidJUnit4::class)
internal abstract class BaseBackendIntegrationTest {

    companion object {
        private val TIMEOUT = 10.seconds

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            if (!canRunIntegrationTests()) {
                error("You need to set required constants in Constants.kt")
            }
        }

        private fun canRunIntegrationTests() = Constants.apiKey != "REVENUECAT_API_KEY"
            && Constants.loadShedderApiKey != "LOAD_SHEDDER_API_KEY"
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
        setupTest()
    }

    abstract fun apiKey(): String

    protected fun setupTest(
        signatureVerificationMode: SignatureVerificationMode = SignatureVerificationMode.Disabled
    ) {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns URL("https://api.revenuecat.com")
            every { store } returns Store.PLAY_STORE
            every { platformInfo } returns PlatformInfo("test-flavor", version = null)
            every { languageTag } returns "en-US"
            every { versionName } returns "test-version-name"
            every { packageName } returns "com.revenuecat.purchases.backend_tests"
            every { customEntitlementsComputation } returns false
            every { finishTransactions } returns true
            every { forceServerErrors } returns false
            every { forceSigningErrors } returns false
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
        signingManager = spyk(SigningManager(signatureVerificationMode, appConfig, apiKey()))
        httpClient = HTTPClient(appConfig, eTagManager, diagnosticsTrackerIfEnabled = null, signingManager)
        backendHelper = BackendHelper(apiKey(), dispatcher, appConfig, httpClient)
        backend = Backend(appConfig, dispatcher, diagnosticsDispatcher, httpClient, backendHelper)
    }

    protected fun ensureBlockFinishes(block: (CountDownLatch) -> Unit) {
        val latch = CountDownLatch(1)
        block(latch)
        latch.await(TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(latch.count).isEqualTo(0)
    }

    protected fun assertSigningPerformed() {
        verify(exactly = 1) { signingManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())  }
    }

    protected fun assertSigningNotPerformed() {
        verify(exactly = 0) { signingManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())  }
    }
}
