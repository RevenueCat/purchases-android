package com.revenuecat.purchases

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import androidx.core.os.UserManagerCompat
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.SharedPreferencesManager
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsFileHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.common.isDeviceProtectedStorageCompat
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
import com.revenuecat.purchases.paywalls.FontLoader
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.Emojis
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesPoster
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.CoilImageDownloader
import com.revenuecat.purchases.utils.IsDebugBuildProvider
import com.revenuecat.purchases.utils.OfferingImagePreDownloader
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencyManager
import kotlinx.coroutines.Dispatchers
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal class PurchasesFactory(
    private val isDebugBuild: IsDebugBuildProvider,
    private val apiKeyValidator: APIKeyValidator = APIKeyValidator(),
    private val isSimulatedStoreEnabled: () -> Boolean = { BuildConfig.ENABLE_SIMULATED_STORE },
) {

    @Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
    fun createPurchases(
        configuration: PurchasesConfiguration,
        platformInfo: PlatformInfo,
        proxyURL: URL?,
        overrideBillingAbstract: BillingAbstract? = null,
        forceServerErrors: Boolean = false,
        forceSigningError: Boolean = false,
        runningIntegrationTests: Boolean = false,
    ): Purchases {
        val apiKeyValidationResult = validateConfiguration(configuration)

        with(configuration) {
            val finalStore = if (
                apiKeyValidationResult == APIKeyValidator.ValidationResult.SIMULATED_STORE && isSimulatedStoreEnabled()
            ) {
                Store.UNKNOWN_STORE // We should add a new store when we fully support the simulated store.
            } else {
                store
            }

            val application = context.getApplication()
            val appConfig = AppConfig(
                context,
                purchasesAreCompletedBy,
                showInAppMessagesAutomatically,
                platformInfo,
                proxyURL,
                finalStore,
                isDebugBuild(),
                apiKeyValidationResult,
                dangerousSettings,
                runningIntegrationTests,
                forceServerErrors,
                forceSigningError,
            )

            val contextForStorage = if (context.isDeviceProtectedStorageCompat) {
                @Suppress("MaxLineLength")
                debugLog {
                    "${Emojis.DOUBLE_EXCLAMATION} Using device-protected storage. Make sure to *always* configure " +
                        "Purchases with a Context object created using `createDeviceProtectedStorageContext()` to " +
                        "avoid undefined behavior.\nSee " +
                        "https://developer.android.com/reference/android/content/Context#createDeviceProtectedStorageContext() " +
                        "for more info."
                }
                context
            } else {
                application
            }

            val prefs = try {
                SharedPreferencesManager(contextForStorage).getSharedPreferences()
            } catch (e: IllegalStateException) {
                @Suppress("MaxLineLength")
                if (!UserManagerCompat.isUserUnlocked(context)) {
                    throw IllegalStateException(
                        "Trying to configure Purchases while the device is locked. If you need to support this " +
                            "scenario, ensure you *always* configure Purchases with a Context created with " +
                            "`createDeviceProtectedStorageContext()` to avoid undefined behavior.\nSee " +
                            "https://developer.android.com/reference/android/content/Context#createDeviceProtectedStorageContext() " +
                            "for more info.",
                        e,
                    )
                } else {
                    throw e
                }
            }

            val eTagManager = ETagManager(contextForStorage)

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
                diagnosticsFileHelper = DiagnosticsFileHelper(FileHelper(contextForStorage))
                diagnosticsHelper = DiagnosticsHelper(contextForStorage, diagnosticsFileHelper)
                diagnosticsTracker = DiagnosticsTracker(
                    appConfig,
                    diagnosticsFileHelper,
                    diagnosticsHelper,
                    eventsDispatcher,
                )
            } else if (diagnosticsEnabled) {
                warnLog { "Diagnostics are only supported on Android N or newer." }
            }

            val signatureVerificationMode = try {
                SignatureVerificationMode.fromEntitlementVerificationMode(
                    verificationMode,
                )
            } catch (e: IllegalStateException) {
                // If we're not able to create the signature verifier, we should disable signature verification
                // instead of crashing
                errorLog { "Error creating signature verifier: ${e.message}. Disabling signature verification." }
                SignatureVerificationMode.Disabled
            }
            val signingManager = SigningManager(signatureVerificationMode, appConfig, apiKey)

            val cache = DeviceCache(prefs, apiKey, Dispatchers.IO)

            val localeProvider = DefaultLocaleProvider()
            val httpClient = HTTPClient(
                appConfig,
                eTagManager,
                diagnosticsTracker,
                signingManager,
                cache,
                localeProvider = localeProvider,
            )
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
                finalStore,
                application,
                backendHelper,
                cache,
                purchasesAreCompletedBy.finishTransactions,
                diagnosticsTracker,
                purchasesStateProvider,
                pendingTransactionsForPrepaidPlansEnabled,
                backend,
                apiKeyValidationResult,
            )

            val subscriberAttributesPoster = SubscriberAttributesPoster(backendHelper)

            val attributionFetcher = AttributionFetcherFactory.createAttributionFetcher(store, backendDispatcher)

            val subscriberAttributesCache = SubscriberAttributesCache(cache)

            val subscriberAttributesManager = SubscriberAttributesManager(
                subscriberAttributesCache,
                subscriberAttributesPoster,
                attributionFetcher,
                automaticDeviceIdentifierCollectionEnabled,
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

            val offeringsCache = OfferingsCache(
                deviceCache = cache,
                localeProvider = localeProvider,
            )

            val identityManager = IdentityManager(
                cache,
                subscriberAttributesCache,
                subscriberAttributesManager,
                offeringsCache,
                backend,
                offlineEntitlementsManager,
                dispatcher,
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
                diagnosticsTracker,
            )
            val offeringParser = OfferingParserFactory.createOfferingParser(finalStore, apiKeyValidationResult)

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
                diagnosticsTracker.listener = diagnosticsSynchronizer
            }

            val syncPurchasesHelper = SyncPurchasesHelper(
                billing,
                identityManager,
                customerInfoHelper,
                postReceiptHelper,
                diagnosticsTracker,
            )

            val fontLoader = FontLoader(
                context = contextForStorage,
            )

            val offeringFontPreDownloader = OfferingFontPreDownloader(
                context = contextForStorage,
                fontLoader = fontLoader,
            )

            val offeringsManager = OfferingsManager(
                offeringsCache,
                backend,
                OfferingsFactory(billing, offeringParser, dispatcher),
                OfferingImagePreDownloader(coilImageDownloader = CoilImageDownloader(application)),
                diagnosticsTracker,
                offeringFontPreDownloader = offeringFontPreDownloader,
            )

            log(LogIntent.DEBUG) { ConfigureStrings.DEBUG_ENABLED }
            log(LogIntent.DEBUG) { ConfigureStrings.SDK_VERSION.format(Purchases.frameworkVersion) }
            log(LogIntent.DEBUG) { ConfigureStrings.PACKAGE_NAME.format(appConfig.packageName) }
            log(LogIntent.USER) { ConfigureStrings.INITIAL_APP_USER_ID.format(appUserID) }
            log(LogIntent.DEBUG) {
                ConfigureStrings.VERIFICATION_MODE_SELECTED.format(configuration.verificationMode.name)
            }

            val virtualCurrencyManager = VirtualCurrencyManager(
                identityManager = identityManager,
                deviceCache = cache,
                backend = backend,
                appConfig = appConfig,
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
                diagnosticsTracker,
                offlineEntitlementsManager = offlineEntitlementsManager,
                postReceiptHelper = postReceiptHelper,
                postTransactionWithProductDetailsHelper = postTransactionWithProductDetailsHelper,
                postPendingTransactionsHelper = postPendingTransactionsHelper,
                syncPurchasesHelper = syncPurchasesHelper,
                offeringsManager = offeringsManager,
                eventsManager = createEventsManager(application, identityManager, eventsDispatcher, backend),
                paywallPresentedCache = paywallPresentedCache,
                purchasesStateCache = purchasesStateProvider,
                dispatcher = dispatcher,
                initialConfiguration = configuration,
                fontLoader = fontLoader,
                localeProvider = localeProvider,
                virtualCurrencyManager = virtualCurrencyManager,
            )

            return Purchases(purchasesOrchestrator)
        }
    }

    private fun createEventsManager(
        context: Context,
        identityManager: IdentityManager,
        eventsDispatcher: Dispatcher,
        backend: Backend,
    ): EventsManager? {
        // RevenueCatUI is Android 24+ so it should always enter here when using RevenueCatUI.
        // Still, we check for Android N or newer since we use Streams which are 24+ and the main SDK supports
        // older versions.
        return if (isAndroidNOrNewer()) {
            EventsManager(
                legacyEventsFileHelper = EventsManager.paywalls(fileHelper = FileHelper(context)),
                fileHelper = EventsManager.backendEvents(fileHelper = FileHelper(context)),
                identityManager = identityManager,
                eventsDispatcher = eventsDispatcher,
                postEvents = { request, onSuccess, onError ->
                    backend.postEvents(
                        paywallEventRequest = request,
                        onSuccessHandler = onSuccess,
                        onErrorHandler = onError,
                    )
                },
            )
        } else {
            debugLog { "Paywall events are only supported on Android N or newer." }
            null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun validateConfiguration(configuration: PurchasesConfiguration): APIKeyValidator.ValidationResult {
        with(configuration) {
            require(context.hasPermission(Manifest.permission.INTERNET)) {
                "Purchases requires INTERNET permission."
            }

            require(apiKey.isNotBlank()) { "API key must be set. Get this from the RevenueCat web app" }

            val apiKeyValidationResult = apiKeyValidator.validateAndLog(apiKey, store)

            if (!isDebugBuild() &&
                apiKeyValidationResult == APIKeyValidator.ValidationResult.SIMULATED_STORE && isSimulatedStoreEnabled()
            ) {
                throw PurchasesException(
                    PurchasesError(
                        code = PurchasesErrorCode.ConfigurationError,
                        underlyingErrorMessage = "Please configure the Play Store/Amazon store app on the " +
                            "RevenueCat dashboard and use its corresponding API key before releasing.",
                    ),
                )
            }

            require(context.applicationContext is Application) { "Needs an application context." }

            return apiKeyValidationResult
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
