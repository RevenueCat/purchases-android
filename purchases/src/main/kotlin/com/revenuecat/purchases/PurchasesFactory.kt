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
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsAnonymizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsFileHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offerings.OfferingsFactory
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineCustomerInfoCalculator
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.offlineentitlements.PurchasedProductsFetcher
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.paywalls.events.PaywallEventsManager
import com.revenuecat.purchases.paywalls.events.PaywallStoredEvent
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesPoster
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.CoilImageDownloader
import com.revenuecat.purchases.utils.EventsFileHelper
import com.revenuecat.purchases.utils.OfferingImagePreDownloader
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal class PurchasesFactory(
    private val apiKeyValidator: APIKeyValidator = APIKeyValidator(),
) {

    @Suppress("LongMethod", "LongParameterList")
    fun createPurchases(
        configuration: PurchasesConfiguration,
        platformInfo: PlatformInfo,
        proxyURL: URL?,
        overrideBillingAbstract: BillingAbstract? = null,
        forceServerErrors: Boolean = false,
        forceSigningError: Boolean = false,
        runningIntegrationTests: Boolean = false,
    ): Purchases {
        validateConfiguration(configuration)

        with(configuration) {
            val application = context.getApplication()
            val appConfig = AppConfig(
                context,
                observerMode,
                showInAppMessagesAutomatically,
                platformInfo,
                proxyURL,
                store,
                dangerousSettings,
                runningIntegrationTests,
                forceServerErrors,
                forceSigningError,
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(application)

            val eTagManager = ETagManager(context)

            val dispatcher = Dispatcher(createDefaultExecutor(), runningIntegrationTests = runningIntegrationTests)
            val backendDispatcher = Dispatcher(
                service ?: createDefaultExecutor(),
                runningIntegrationTests = runningIntegrationTests,
            )
            val eventsDispatcher = Dispatcher(
                createEventsExecutor(),
                runningIntegrationTests = runningIntegrationTests,
            )

            var diagnosticsFileHelper: DiagnosticsFileHelper? = null
            var diagnosticsHelper: DiagnosticsHelper? = null
            var diagnosticsTracker: DiagnosticsTracker? = null
            if (diagnosticsEnabled && isAndroidNOrNewer()) {
                diagnosticsFileHelper = DiagnosticsFileHelper(FileHelper(context))
                diagnosticsHelper = DiagnosticsHelper(context, diagnosticsFileHelper)
                diagnosticsTracker = DiagnosticsTracker(
                    appConfig,
                    diagnosticsFileHelper,
                    DiagnosticsAnonymizer(Anonymizer()),
                    diagnosticsHelper,
                    eventsDispatcher,
                )
            } else if (diagnosticsEnabled) {
                warnLog("Diagnostics are only supported on Android N or newer.")
            }

            val signatureVerificationMode = SignatureVerificationMode.fromEntitlementVerificationMode(
                verificationMode,
            )
            val signingManager = SigningManager(signatureVerificationMode, appConfig, apiKey)

            val cache = DeviceCache(prefs, apiKey)

            val httpClient = HTTPClient(appConfig, eTagManager, diagnosticsTracker, signingManager, cache)
            val backendHelper = BackendHelper(apiKey, backendDispatcher, appConfig, httpClient)
            val backend = Backend(
                appConfig,
                backendDispatcher,
                eventsDispatcher,
                httpClient,
                backendHelper,
            )

            val purchasesStateProvider = PurchasesStateCache(PurchasesState())

            // Override used for integration tests.
            val billing: BillingAbstract = overrideBillingAbstract ?: BillingFactory.createBilling(
                store,
                application,
                backendHelper,
                cache,
                observerMode,
                diagnosticsTracker,
                purchasesStateProvider,
                pendingTransactionsForPrepaidPlansEnabled,
            )

            val subscriberAttributesPoster = SubscriberAttributesPoster(backendHelper)

            val attributionFetcher = AttributionFetcherFactory.createAttributionFetcher(store, backendDispatcher)

            val subscriberAttributesCache = SubscriberAttributesCache(cache)

            val subscriberAttributesManager = SubscriberAttributesManager(
                subscriberAttributesCache,
                subscriberAttributesPoster,
                attributionFetcher,
            )

            val offlineCustomerInfoCalculator = OfflineCustomerInfoCalculator(
                PurchasedProductsFetcher(cache, billing),
                appConfig,
                diagnosticsTracker,
            )

            val offlineEntitlementsManager = OfflineEntitlementsManager(
                backend,
                offlineCustomerInfoCalculator,
                cache,
                appConfig,
                diagnosticsTracker,
            )

            val offeringsCache = OfferingsCache(cache)

            val identityManager = IdentityManager(
                cache,
                subscriberAttributesCache,
                subscriberAttributesManager,
                offeringsCache,
                backend,
                offlineEntitlementsManager,
            )

            val customerInfoUpdateHandler = CustomerInfoUpdateHandler(
                cache,
                identityManager,
                offlineEntitlementsManager,
                appConfig = appConfig,
                diagnosticsTracker = diagnosticsTracker,
            )

            val paywallPresentedCache = PaywallPresentedCache()

            val postReceiptHelper = PostReceiptHelper(
                appConfig,
                backend,
                billing,
                customerInfoUpdateHandler,
                cache,
                subscriberAttributesManager,
                offlineEntitlementsManager,
                paywallPresentedCache,
            )

            val postTransactionWithProductDetailsHelper = PostTransactionWithProductDetailsHelper(
                billing,
                postReceiptHelper,
            )

            val postPendingTransactionsHelper = PostPendingTransactionsHelper(
                appConfig,
                cache,
                billing,
                backendDispatcher,
                identityManager,
                postTransactionWithProductDetailsHelper,
            )

            val customerInfoHelper = CustomerInfoHelper(
                cache,
                backend,
                offlineEntitlementsManager,
                customerInfoUpdateHandler,
                postPendingTransactionsHelper,
            )
            val offeringParser = OfferingParserFactory.createOfferingParser(store)

            var diagnosticsSynchronizer: DiagnosticsSynchronizer? = null
            @Suppress("ComplexCondition")
            if (diagnosticsFileHelper != null &&
                diagnosticsHelper != null &&
                diagnosticsTracker != null &&
                isAndroidNOrNewer()
            ) {
                diagnosticsSynchronizer = DiagnosticsSynchronizer(
                    diagnosticsHelper,
                    diagnosticsFileHelper,
                    diagnosticsTracker,
                    backend,
                    eventsDispatcher,
                )
            }

            val syncPurchasesHelper = SyncPurchasesHelper(
                billing,
                identityManager,
                customerInfoHelper,
                postReceiptHelper,
            )

            val offeringsManager = OfferingsManager(
                offeringsCache,
                backend,
                OfferingsFactory(billing, offeringParser, dispatcher),
                OfferingImagePreDownloader(coilImageDownloader = CoilImageDownloader(application)),
            )

            log(LogIntent.DEBUG, ConfigureStrings.DEBUG_ENABLED)
            log(LogIntent.DEBUG, ConfigureStrings.SDK_VERSION.format(Purchases.frameworkVersion))
            log(LogIntent.DEBUG, ConfigureStrings.PACKAGE_NAME.format(appConfig.packageName))
            log(LogIntent.USER, ConfigureStrings.INITIAL_APP_USER_ID.format(appUserID))
            log(
                LogIntent.DEBUG,
                ConfigureStrings.VERIFICATION_MODE_SELECTED.format(configuration.verificationMode.name),
            )

            val purchasesOrchestrator = PurchasesOrchestrator(
                application,
                appUserID,
                backend,
                billing,
                cache,
                identityManager,
                subscriberAttributesManager,
                appConfig,
                customerInfoHelper,
                customerInfoUpdateHandler,
                diagnosticsSynchronizer,
                offlineEntitlementsManager,
                postReceiptHelper,
                postTransactionWithProductDetailsHelper,
                postPendingTransactionsHelper,
                syncPurchasesHelper,
                offeringsManager,
                createPaywallEventsManager(application, identityManager, eventsDispatcher, backend),
                paywallPresentedCache,
                purchasesStateProvider,
            )

            return Purchases(purchasesOrchestrator)
        }
    }

    private fun createPaywallEventsManager(
        context: Context,
        identityManager: IdentityManager,
        eventsDispatcher: Dispatcher,
        backend: Backend,
    ): PaywallEventsManager? {
        // RevenueCatUI is Android 24+ so it should always enter here when using RevenueCatUI.
        // Still, we check for Android N or newer since we use Streams which are 24+ and the main SDK supports
        // older versions.
        return if (isAndroidNOrNewer()) {
            PaywallEventsManager(
                EventsFileHelper(
                    FileHelper(context),
                    PaywallEventsManager.PAYWALL_EVENTS_FILE_PATH,
                    PaywallStoredEvent::fromString,
                ),
                identityManager,
                eventsDispatcher,
                backend,
            )
        } else {
            debugLog("Paywall events are only supported on Android N or newer.")
            null
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

    private fun createEventsExecutor(): ExecutorService {
        return Executors.newSingleThreadScheduledExecutor(LowPriorityThreadFactory("revenuecat-events-thread"))
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
