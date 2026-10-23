@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.iam

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesFactory
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.identity.AuthenticationDelegate
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService

/**
 * Base for the IAM end-to-end suite: the real SDK from `PurchasesFactory` down, with only the network
 * ([FakeRevenueCatBackend] behind a [MockWebServer] proxy) and the store ([FakeBilling]) standing in.
 * Foreground and background go through the SDK's lifecycle observer, on a lifecycle the test owns.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal abstract class IAMEndToEndTest {

    protected val backend = FakeRevenueCatBackend()
    protected val delegate = RecordingDelegate()
    protected val customerInfoUpdates = CopyOnWriteArrayList<CustomerInfo>()

    private val server = MockWebServer()

    // The suite's own process lifecycle. The real ProcessLifecycleOwner is shared by every test in the JVM, and
    // other tests leave observers on it that would see these foregrounds. A mock owner compiles against both the
    // Java (2.5) and Kotlin (2.6+) LifecycleOwner.
    private val processLifecycleOwner = mockk<LifecycleOwner>()
    private val processLifecycle = LifecycleRegistry(processLifecycleOwner).also { registry ->
        every { processLifecycleOwner.lifecycle } returns registry
    }
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val instances = mutableListOf<Purchases>()

    protected lateinit var purchases: Purchases
    protected lateinit var billing: FakeBilling

    @Before
    fun startBackend() {
        Purchases.logLevel = LogLevel.VERBOSE
        shadowOf(application).grantPermissions(Manifest.permission.INTERNET)
        server.dispatcher = backend
        server.start()
        setProcessLifecycle(Lifecycle.State.CREATED)
    }

    @After
    fun stopBackend() {
        instances.forEach { it.close() }
        idleMainLooper()
        setProcessLifecycle(Lifecycle.State.CREATED)
        server.shutdown()
    }

    /** Configures a new SDK instance against the fake backend, as an app's `Application.onCreate` would. */
    protected fun configure(
        iamEnabled: Boolean = true,
        apiKey: String = API_KEY,
        appUserID: String? = null,
        service: ExecutorService? = null,
    ): Purchases {
        billing = FakeBilling(PurchasesStateCache(PurchasesState()))
        val builder = PurchasesConfiguration.Builder(application, apiKey)
            .appUserID(appUserID)
            .iamEnabled(iamEnabled)
            .diagnosticsEnabled(false)
            // The fake's responses are unsigned. Verifying them fails every response, and failed responses are
            // never ETag-cached.
            .entitlementVerificationMode(EntitlementVerificationMode.DISABLED)
        service?.let { builder.service(it) }
        val configuration = builder.build()
        purchases = PurchasesFactory(isDebugBuild = { true }).createPurchases(
            configuration = configuration,
            platformInfo = PlatformInfo(flavor = "native", version = null),
            proxyURL = server.url("/").toUrl(),
            overrideBillingAbstract = billing,
            runningIntegrationTests = true,
            processLifecycleOwnerProvider = { processLifecycleOwner },
        )
        instances.add(purchases)
        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfoUpdates.add(it) }
        purchases.authentication.delegate = delegate
        idleMainLooper()
        return purchases
    }

    /** A fresh process: the current instance goes away and a new one is configured on the same storage. */
    protected fun relaunch(iamEnabled: Boolean = true, apiKey: String = API_KEY): Purchases {
        setProcessLifecycle(Lifecycle.State.CREATED)
        purchases.close()
        instances.remove(purchases)
        idleMainLooper()
        return configure(iamEnabled = iamEnabled, apiKey = apiKey)
    }

    protected fun foreground() = setProcessLifecycle(Lifecycle.State.STARTED)

    protected fun background() = setProcessLifecycle(Lifecycle.State.CREATED)

    protected val currentAppUserID: String
        get() = purchases.appUserID

    /**
     * Runs main-looper work until [condition] holds. Waits on request arrivals between checks rather than
     * sleeping, so it returns as soon as the SDK makes progress.
     */
    protected fun awaitUntil(description: String, timeoutMs: Long = TIMEOUT_MS, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            idleMainLooper()
            if (condition()) return
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for: $description\n${diagnostics()}" }
            backend.awaitArrival(POLL_MS)
        }
    }

    // What a timeout reports, so a failure on another machine explains itself.
    private fun diagnostics(): String = buildString {
        appendLine("App user ID: ${if (::purchases.isInitialized) currentAppUserID else "not configured"}")
        appendLine("CustomerInfo updates: ${customerInfoUpdates.map { it.originalAppUserId }}")
        appendLine("Delegate: ${delegate.tokens.size} token(s), errors ${delegate.errors}")
        appendLine("Requests (${backend.requests.size}):")
        backend.requests.forEach { appendLine("  ${it.method} ${it.path} as ${it.user() ?: "API key"}") }
    }

    /** Lets in-flight work finish: returns once no request has arrived for [quietMs]. */
    protected fun awaitQuiet(quietMs: Long = QUIET_MS) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            idleMainLooper()
            if (!backend.awaitArrival(quietMs)) {
                idleMainLooper()
                return
            }
        }
        error("The SDK never went quiet")
    }

    /** The thread the last [awaitCustomerInfo] callback ran on. */
    protected var lastCallbackThread: Thread? = null

    /** Calls [start] with a callback and waits for its result. */
    protected fun awaitCustomerInfo(start: (ReceiveCustomerInfoCallback) -> Unit): Result<CustomerInfo> {
        var result: Result<CustomerInfo>? = null
        start(
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    lastCallbackThread = Thread.currentThread()
                    result = Result.success(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    lastCallbackThread = Thread.currentThread()
                    result = Result.failure(PurchasesErrorException(error))
                }
            },
        )
        awaitUntil("a CustomerInfo result") { result != null }
        return result!!
    }

    protected val Result<CustomerInfo>.purchasesError: PurchasesError?
        get() = (exceptionOrNull() as? PurchasesErrorException)?.error

    /** The user each request authenticated as, by its Bearer access token; null for API-key requests. */
    protected fun FakeRevenueCatBackend.Request.user(): String? = backend.ownerOf(bearerToken)

    /** Foregrounds and waits for the silent anonymous login to publish its token. */
    protected fun foregroundAndBootstrap() {
        val logins = backend.requests("/auth/login").size
        foreground()
        awaitUntil("the bootstrap's token") {
            backend.requests("/auth/login").size > logins &&
                backend.ownerOf(delegate.tokens.lastOrNull()) == currentAppUserID
        }
        awaitQuiet()
    }

    /** A callback that hands its result to [onResult], for calls a test starts without waiting on. */
    protected fun recordingCallback(onResult: (Result<CustomerInfo>) -> Unit) = object : ReceiveCustomerInfoCallback {
        override fun onReceived(customerInfo: CustomerInfo) = onResult(Result.success(customerInfo))
        override fun onError(error: PurchasesError) = onResult(Result.failure(PurchasesErrorException(error)))
    }

    /** Hands [onResult] the call's error, or null on success. */
    protected fun offeringsCallback(onResult: (PurchasesError?) -> Unit) = object : ReceiveOfferingsCallback {
        override fun onReceived(offerings: Offerings) = onResult(null)
        override fun onError(error: PurchasesError) = onResult(error)
    }

    /** Hands [onResult] the call's error, or null on success. */
    protected fun syncCallback(onResult: (PurchasesError?) -> Unit) = object : SyncAttributesAndOfferingsCallback {
        override fun onSuccess(offerings: Offerings) = onResult(null)
        override fun onError(error: PurchasesError) = onResult(error)
    }

    protected val isMainThread: Boolean
        get() = Thread.currentThread() == Looper.getMainLooper().thread

    protected fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun setProcessLifecycle(state: Lifecycle.State) {
        processLifecycle.currentState = state
        idleMainLooper()
    }

    class PurchasesErrorException(val error: PurchasesError) : Exception(error.toString())

    class RecordingDelegate : AuthenticationDelegate {
        val errors = CopyOnWriteArrayList<PurchasesError>()
        val tokens = CopyOnWriteArrayList<String?>()

        override fun authenticatorDidEncounterError(error: PurchasesError) {
            errors.add(error)
        }

        override fun authenticatorDidUpdateAccessToken(newAccessToken: String?) {
            tokens.add(newAccessToken)
        }
    }

    companion object {
        const val API_KEY = "goog_iamEndToEndTestKey"
        const val LEGACY_ANONYMOUS_ID = "\$RCAnonymousID:0123456789abcdef0123456789abcdef"
        private const val TIMEOUT_MS = 15_000L
        private const val POLL_MS = 20L
        private const val QUIET_MS = 400L
    }
}
