package com.revenuecat.purchases.common

import android.app.Application
import android.os.Looper
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesOrchestrator
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.blockstore.BlockstoreHelper
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesPoster
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class NativeLoginRecoveryTest : BaseHTTPClientTest() {
    @Test
    fun `silent login returns an error and same identity owner can login again`() {
        exerciseSilentDependency(withUnsyncedAttributes = false)
    }

    @Test
    fun `silent attribute sync releases original login after its native timeout`() {
        exerciseSilentDependency(withUnsyncedAttributes = true)
    }

    private fun awaitCallback(future: CompletableFuture<PurchasesError?>): PurchasesError? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!future.isDone && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        return future.get(1, TimeUnit.MILLISECONDS)
    }

    private fun exerciseSilentDependency(withUnsyncedAttributes: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("native-login-recovery", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val cache = DeviceCache(preferences, "test-api-key")
        val oldUser = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
        val newUser = "onboarding-signed-in-user"
        cache.cacheAppUserID(oldUser)
        val attributesCache = SubscriberAttributesCache(cache)
        val appConfig = createAppConfig(context = context)
        mockSigningManager = SigningManager(SignatureVerificationMode.Disabled, appConfig, "test-api-key")
        client = createClient(appConfig = appConfig, eTagManager = ETagManager(context))
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "native-login-recovery")
        }
        val dispatcher = Dispatcher(executor)
        val helper = BackendHelper("test-api-key", dispatcher, appConfig, client)
        val backend = Backend(appConfig, dispatcher, dispatcher, client, helper)
        val attributes = SubscriberAttributesManager(
            attributesCache, SubscriberAttributesPoster(helper), mockk(), false,
        )
        val identity = IdentityManager(
            appConfig, cache, attributesCache, attributes,
            mockk(relaxed = true), mockk(relaxed = true), backend,
            mockk(relaxed = true), dispatcher, mockk(relaxed = true),
        )
        val blockstore = mockk<BlockstoreHelper>(relaxed = true) {
            every { clearUserIdBackupIfNeeded(any()) } answers { firstArg<() -> Unit>().invoke() }
        }
        val purchases = Purchases(PurchasesOrchestrator(
            application = context.applicationContext as Application,
            backingFieldAppUserID = oldUser,
            backend = backend,
            billing = mockk(relaxed = true),
            deviceCache = cache,
            identityManager = identity,
            subscriberAttributesManager = attributes,
            appConfig = appConfig,
            customerInfoHelper = mockk(relaxed = true),
            customerInfoUpdateHandler = mockk(relaxed = true),
            diagnosticsSynchronizer = null,
            diagnosticsTrackerIfEnabled = null,
            offlineEntitlementsManager = mockk(relaxed = true),
            postReceiptHelper = mockk(relaxed = true),
            postTransactionWithProductDetailsHelper = mockk(relaxed = true),
            postPendingTransactionsHelper = mockk(relaxed = true),
            syncPurchasesHelper = mockk(relaxed = true),
            offeringsManager = mockk(relaxed = true),
            eventsManager = mockk(relaxed = true),
            adEventsManager = mockk(relaxed = true),
            paywallPresentedCache = PaywallPresentedCache(),
            purchasesStateCache = PurchasesStateCache(PurchasesState()),
            dispatcher = dispatcher,
            initialConfiguration = PurchasesConfiguration.Builder(context, "test-api-key").build(),
            fontLoader = mockk(relaxed = true),
            localeProvider = DefaultLocaleProvider(),
            virtualCurrencyManager = mockk(relaxed = true),
            purchaseParamsValidator = mockk(relaxed = true),
            workflowManager = mockk(relaxed = true),
            processLifecycleOwnerProvider = { mockk(relaxed = true) },
            blockstoreHelper = blockstore,
            backupManager = mockk(relaxed = true),
            remoteConfigManager = mockk(relaxed = true),
            uiConfigProvider = mockk(relaxed = true),
            workflowsConfigProvider = mockk(relaxed = true),
            checkpointsConfigProvider = mockk(relaxed = true),
            audiencesConfigProvider = mockk(relaxed = true),
            tokenManager = mockk(relaxed = true),
        ))
        if (withUnsyncedAttributes) attributes.setAttributes(mapOf("campaign" to "onboarding"), oldUser)
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE })
        server.enqueue(MockResponse().setResponseCode(201).setBody(Responses.validFullPurchaserResponse))
        val callbackCount = AtomicInteger()
        val received = mutableListOf<CustomerInfo>()
        fun login(): CompletableFuture<PurchasesError?> {
            val completion = CompletableFuture<PurchasesError?>()
            purchases.logIn(newUser, object : LogInCallback {
                override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                    received.add(customerInfo)
                    callbackCount.incrementAndGet()
                    completion.complete(null)
                }
                override fun onError(error: PurchasesError) {
                    callbackCount.incrementAndGet()
                    completion.complete(error)
                }
            })
            return completion
        }
        try {
            val completion = login()
            val request = server.takeRequest(2, TimeUnit.SECONDS)
            val expectedPath = if (withUnsyncedAttributes) {
                "/v1/subscribers/${Uri.encode(oldUser)}/attributes"
            } else {
                "/v1/subscribers/identify"
            }
            assertThat(request?.path).isEqualTo(expectedPath)
            assertThat(identity.currentAppUserID).isEqualTo(oldUser)
            assertThat(completion.isDone).isFalse()
            val joined = if (withUnsyncedAttributes) null else login()
            val error = awaitCallback(completion)
            val subsequentRequest = if (withUnsyncedAttributes) server.takeRequest(2, TimeUnit.SECONDS) else null
            println("Native completion: $error; subsequent request: ${subsequentRequest?.path}; requests=${server.requestCount}")
            if (withUnsyncedAttributes) {
                assertThat(error).isNull()
            } else {
                assertThat(error?.code).isEqualTo(PurchasesErrorCode.NetworkError)
                assertThat(identity.currentAppUserID).isEqualTo(oldUser)
                assertThat(awaitCallback(joined!!)?.code).isEqualTo(PurchasesErrorCode.NetworkError)
                assertThat(callbackCount.get()).isEqualTo(2)
                assertThat(server.requestCount).isEqualTo(1)
                assertThat(awaitCallback(login())).isNull()
            }
            assertThat((if (withUnsyncedAttributes) subsequentRequest else server.takeRequest(2, TimeUnit.SECONDS))?.path).isEqualTo("/v1/subscribers/identify")
            assertThat(identity.currentAppUserID).isEqualTo(newUser)
            assertThat(cache.getCachedCustomerInfo(newUser)).isNotNull()
            assertThat(purchases.appUserID).isEqualTo(newUser)
            assertThat(received).hasSize(1)
            assertThat(callbackCount.get()).isEqualTo(if (withUnsyncedAttributes) 1 else 3)
        } finally {
            server.shutdown()
            dispatcher.close()
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue()
        }
    }
}
