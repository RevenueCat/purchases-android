package com.revenuecat.purchases.backend_integration_tests

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.caching.DeviceCache
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
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

// To run these tests in Android Studio, you need to remove the test exclusion in the
// common module build.gradle and change the API KEY in Constants.kt
@RunWith(AndroidJUnit4::class)
internal abstract class BaseBackendIntegrationTest {

    public companion object {
        private val TIMEOUT = 10.seconds
        private val GOLDEN_FILES_BASE_DIR = File("src/test/resources/backend_integration_tests_golden")

        @BeforeClass
        @JvmStatic
        public fun setupClass() {
            if (!canRunIntegrationTests()) {
                error("You need to set required constants in Constants.kt")
            }
        }

        private fun canRunIntegrationTests() = Constants.apiKey != "REVENUECAT_API_KEY"
            && Constants.loadShedderApiKey != "LOAD_SHEDDER_API_KEY"
    }

    @get:Rule
    val testName = TestName()

    lateinit var appConfig: AppConfig
    lateinit var dispatcher: Dispatcher
    lateinit var diagnosticsDispatcher: Dispatcher
    lateinit var sharedPreferences: SharedPreferences
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    lateinit var eTagManager: ETagManager
    lateinit var signingManager: SigningManager
    lateinit var httpClient: HTTPClient
    lateinit var backendHelper: BackendHelper
    lateinit var deviceCache: DeviceCache
    lateinit var goldenFileRecorder: GoldenFileRecorder

    lateinit var backend: Backend

    @Before
    public fun setUp() {
        goldenFileRecorder = GoldenFileRecorder(
            className = this::class.simpleName ?: "UnknownClass",
            testName = testName.methodName,
            baseDirectory = GOLDEN_FILES_BASE_DIR
        )
        setupTest()
    }

    abstract fun apiKey(): String
    open val forceServerErrorStrategy: ForceServerErrorStrategy? = null

    protected fun setupTest(
        signatureVerificationMode: SignatureVerificationMode = SignatureVerificationMode.Disabled
    ) {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns URL("https://api.revenuecat.com")
            every { store } returns Store.PLAY_STORE
            every { isDebugBuild } returns true
            every { platformInfo } returns PlatformInfo("test-flavor", version = null)
            every { languageTag } returns "en-US"
            every { versionName } returns "test-version-name"
            every { packageName } returns "com.revenuecat.purchases.backend_tests"
            every { customEntitlementComputation } returns false
            every { finishTransactions } returns true
            every { forceSigningErrors } returns false
            every { isAppBackgrounded } returns false
            every { fallbackBaseURLs } returns listOf(AppConfig.fallbackURL)
            every { runningTests } returns true
        }
        dispatcher = Dispatcher(Executors.newSingleThreadScheduledExecutor(), runningIntegrationTests = true)
        diagnosticsDispatcher = Dispatcher(Executors.newSingleThreadScheduledExecutor(), runningIntegrationTests = true)
        sharedPreferencesEditor = mockk<SharedPreferences.Editor>().apply {
            every { putString(any(), any()) } returns this
            every { apply() } just Runs
        }
        sharedPreferences = mockk<SharedPreferences>().apply {
            every { getString(any(), any()) } answers { secondArg() as String? }
            every { edit() } returns sharedPreferencesEditor
        }
        eTagManager = ETagManager(mockk(), lazy { sharedPreferences })
        signingManager = spyk(SigningManager(signatureVerificationMode, appConfig, apiKey()))
        deviceCache = DeviceCache(sharedPreferences, apiKey())
        httpClient = HTTPClient(
            appConfig,
            eTagManager,
            diagnosticsTrackerIfEnabled = null,
            signingManager,
            deviceCache,
            localeProvider = DefaultLocaleProvider(),
            forceServerErrorStrategy = forceServerErrorStrategy,
            requestResponseListener = goldenFileRecorder
        )
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
