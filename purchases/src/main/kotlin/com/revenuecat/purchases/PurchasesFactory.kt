package com.revenuecat.purchases

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.Anonymizer
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsAnonymizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsFileHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesPoster
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal class PurchasesFactory(
    private val apiKeyValidator: APIKeyValidator = APIKeyValidator(),
) {

    @Suppress("LongMethod")
    fun createPurchases(
        configuration: PurchasesConfiguration,
        platformInfo: PlatformInfo,
        proxyURL: URL?,
        overrideBillingAbstract: BillingAbstract? = null
    ): Purchases {
        validateConfiguration(configuration)

        with(configuration) {
            val application = context.getApplication()
            val appConfig = AppConfig(
                context,
                observerMode,
                platformInfo,
                proxyURL,
                store,
                dangerousSettings
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(application)

            val sharedPreferencesForETags = ETagManager.initializeSharedPreferences(context)
            val eTagManager = ETagManager(sharedPreferencesForETags)

            val dispatcher = Dispatcher(service ?: createDefaultExecutor())
            val diagnosticsDispatcher = Dispatcher(createDiagnosticsExecutor())

            var diagnosticsFileHelper: DiagnosticsFileHelper? = null
            var diagnosticsTracker: DiagnosticsTracker? = null
            if (diagnosticsEnabled) {
                diagnosticsFileHelper = DiagnosticsFileHelper(FileHelper(context))
                diagnosticsTracker = DiagnosticsTracker(
                    diagnosticsFileHelper,
                    DiagnosticsAnonymizer(Anonymizer()),
                    diagnosticsDispatcher
                )
            }

            // Trusted entitlements: Commented out until ready to be made public
//            val signatureVerificationMode = SignatureVerificationMode.fromEntitlementVerificationMode(
//                verificationMode
//            )
            val signatureVerificationMode = SignatureVerificationMode.Disabled
            val signingManager = SigningManager(signatureVerificationMode)

            val httpClient = HTTPClient(appConfig, eTagManager, diagnosticsTracker, signingManager)
            val backendHelper = BackendHelper(apiKey, dispatcher, appConfig, httpClient)
            val backend = Backend(
                appConfig,
                dispatcher,
                diagnosticsDispatcher,
                httpClient,
                backendHelper
            )
            val cache = DeviceCache(prefs, apiKey)

            // Override used for integration tests.
            val billing: BillingAbstract = overrideBillingAbstract ?: BillingFactory.createBilling(
                store,
                application,
                backendHelper,
                cache,
                observerMode,
                diagnosticsTracker
            )

            val subscriberAttributesPoster = SubscriberAttributesPoster(backendHelper)

            val attributionFetcher = AttributionFetcherFactory.createAttributionFetcher(store, dispatcher)

            val subscriberAttributesCache = SubscriberAttributesCache(cache)

            val subscriberAttributesManager = SubscriberAttributesManager(
                subscriberAttributesCache,
                subscriberAttributesPoster,
                attributionFetcher
            )

            val identityManager = IdentityManager(
                cache,
                subscriberAttributesCache,
                subscriberAttributesManager,
                backend
            )

            val customerInfoHelper = CustomerInfoHelper(cache, backend, identityManager)
            val offeringParser = OfferingParserFactory.createOfferingParser(store)

            var diagnosticsSynchronizer: DiagnosticsSynchronizer? = null
            if (diagnosticsFileHelper != null && diagnosticsTracker != null) {
                diagnosticsSynchronizer = DiagnosticsSynchronizer(
                    diagnosticsFileHelper,
                    diagnosticsTracker,
                    backend,
                    diagnosticsDispatcher,
                    DiagnosticsSynchronizer.initializeSharedPreferences(context)
                )
            }

            val offlineEntitlementsManager = OfflineEntitlementsManager(backend, cache)

            val postReceiptHelper = PostReceiptHelper(
                appConfig,
                backend,
                billing,
                customerInfoHelper,
                cache,
                subscriberAttributesManager
            )

            return Purchases(
                application,
                appUserID,
                backend,
                billing,
                cache,
                dispatcher,
                identityManager,
                subscriberAttributesManager,
                appConfig,
                customerInfoHelper,
                offeringParser,
                diagnosticsSynchronizer,
                offlineEntitlementsManager,
                postReceiptHelper
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun validateConfiguration(configuration: PurchasesConfiguration) {
        with(configuration) {
            require(context.hasPermission(Manifest.permission.INTERNET)) {
                "Purchases requires INTERNET permission."
            }

            require(apiKey.isNotBlank()) { "API key must be set. Get this from the RevenueCat web app" }

            require(context.applicationContext is Application) { "Needs an application context." }

            apiKeyValidator.validateAndLog(apiKey, store)
        }
    }

    private fun Context.getApplication() = applicationContext as Application

    private fun Context.hasPermission(permission: String): Boolean {
        return checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun createDefaultExecutor(): ExecutorService {
        return Executors.newSingleThreadScheduledExecutor()
    }

    private fun createDiagnosticsExecutor(): ExecutorService {
        return Executors.newSingleThreadScheduledExecutor(LowPriorityThreadFactory("revenuecat-diagnostics-thread"))
    }

    private class LowPriorityThreadFactory(private val threadName: String) : ThreadFactory {
        override fun newThread(r: Runnable?): Thread {
            val wrapperRunnable = Runnable {
                r?.let {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST)
                    r.run()
                }
            }
            return Thread(wrapperRunnable, threadName)
        }
    }
}
