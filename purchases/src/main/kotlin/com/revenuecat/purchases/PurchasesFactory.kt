package com.revenuecat.purchases

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import androidx.core.os.UserManagerCompat
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
import com.revenuecat.purchases.common.audiences.AudiencesConfigProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.LocalTransactionMetadataStore
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsFileHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsHelper
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.common.isDeviceProtectedStorageCompat
import com.revenuecat.purchases.common.localrules.DeviceDimensionProvider
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesEngineLoggerBridge
import com.revenuecat.purchases.common.localrules.StoreDimensionProvider
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.networking.APISourceFailover
import com.revenuecat.purchases.common.networking.DeviceConnectivityChecker
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPTimeoutManager
import com.revenuecat.purchases.common.networking.SourceHealthChecker
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offerings.OfferingsFactory
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineCustomerInfoCalculator
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.offlineentitlements.PurchasedProductsFetcher
import com.revenuecat.purchases.common.remoteconfig.DefaultRemoteConfigSourceProvider
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobFetcher
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDiskCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopicStore
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.common.workflows.WorkflowAssetPrewarmer
import com.revenuecat.purchases.common.workflows.WorkflowManager
import com.revenuecat.purchases.common.workflows.WorkflowsConfigProvider
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.paywalls.FontLoader
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.paywalls.events.PaywallStoredEvent
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.Emojis
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesPoster
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.DefaultUrlConnectionFactory
import com.revenuecat.purchases.utils.EventsFileHelper
import com.revenuecat.purchases.utils.IsDebugBuildProvider
import com.revenuecat.purchases.utils.OfferingImagePreDownloader
import com.revenuecat.purchases.utils.OfferingWebViewPrewarmer
import com.revenuecat.purchases.utils.PurchaseParamsValidator
import com.revenuecat.purchases.utils.UrlConnectionFactory
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import com.revenuecat.purchases.utils.prewarmTargetOfferingIds
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencyManager
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal class PurchasesFactory(
    private val isDebugBuild: IsDebugBuildProvider,
    private val apiKeyValidator: APIKeyValidator = APIKeyValidator(),
) {

    @OptIn(
        ExperimentalPreviewRevenueCatPurchasesAPI::class,
        InternalRevenueCatAPI::class,
    )
    @Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
    fun createPurchases(
        configuration: PurchasesConfiguration,
        platformInfo: PlatformInfo,
        proxyURL: URL?,
        overrideBillingAbstract: BillingAbstract? = null,
        forceServerErrorStrategy: ForceServerErrorStrategy? = null,
        forceSigningError: Boolean = false,
        runningIntegrationTests: Boolean = false,
        baseUrlString: String = AppConfig.baseUrlString,
    ): Purchases {
        val apiKeyValidationResult = validateConfiguration(configuration)

        with(configuration) {
            val finalStore = if (
                apiKeyValidationResult == APIKeyValidator.ValidationResult.SIMULATED_STORE
            ) {
                Store.TEST_STORE
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
                forceSigningError,
                baseUrlString = baseUrlString,
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
            // `/v1/config` gets its own thread so it overlaps `getOfferings` instead of serializing behind it
            // on the backend dispatcher. Kept separate even when the app supplied its own `service`, since
            // sharing that executor would re-serialize the two requests.
            val remoteConfigDispatcher = Dispatcher(
                createRemoteConfigExecutor(),
                runningIntegrationTests = runningIntegrationTests,
            )

            var diagnosticsFileHelper: DiagnosticsFileHelper? = null
            var diagnosticsHelper: DiagnosticsHelper? = null
            var diagnosticsTracker: DiagnosticsTracker? = null
            if (shouldInitializeDiagnostics(diagnosticsEnabled, appConfig.uiPreviewMode) && isAndroidNOrNewer()) {
                diagnosticsFileHelper = DiagnosticsFileHelper(FileHelper(contextForStorage))
                diagnosticsHelper = DiagnosticsHelper(contextForStorage, diagnosticsFileHelper)
                diagnosticsTracker = DiagnosticsTracker(
                    appConfig,
                    diagnosticsFileHelper,
                    diagnosticsHelper,
                    eventsDispatcher,
                )
            } else if (shouldInitializeDiagnostics(diagnosticsEnabled, appConfig.uiPreviewMode)) {
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

            val cache = DeviceCache(prefs, apiKey)

            val localeProvider = DefaultLocaleProvider()

            // The config layer is on everywhere except the customEntitlementComputation flavor, which doesn't
            // serve paywalls this way. Workflows (multipage paywalls) are served from `/v1/config`, so the
            // manager exists wherever the config layer does.
            val remoteConfigEnabled = !appConfig.customEntitlementComputation
            val remoteConfigDiskCache = if (remoteConfigEnabled) RemoteConfigDiskCache(contextForStorage) else null
            val remoteConfigTopicStore = RemoteConfigTopicStore {
                remoteConfigDiskCache?.read()?.topics?.get(it.wireName)
            }
            val apiSourceProvider = DefaultRemoteConfigSourceProvider(remoteConfigTopicStore)
            val apiSourceFailover = APISourceFailover(
                appConfig,
                apiSourceProvider,
                SourceHealthChecker(),
                DeviceConnectivityChecker(application),
            )

            val timeoutManager = HTTPTimeoutManager(appConfig)
            val httpClient = HTTPClient(
                appConfig,
                eTagManager,
                diagnosticsTracker,
                signingManager,
                cache,
                apiSourceFailover,
                localeProvider = localeProvider,
                forceServerErrorStrategy = forceServerErrorStrategy,
                timeoutManager = timeoutManager,
            )
            val backendHelper = BackendHelper(apiKey, backendDispatcher, appConfig, httpClient)
            val backend = Backend(
                appConfig,
                backendDispatcher,
                eventsDispatcher,
                httpClient,
                backendHelper,
                remoteConfigDispatcher,
            )
            val fileRepository = DefaultFileRepository(application)
            val paywallAssetWarming = PaywallAssetWarming(application)

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
                appConfig.applyObfuscatedAccountIdToSubscriptionChanges,
                pendingTransactionsForPrepaidPlansEnabled,
                configuration.galaxyBillingMode,
                backend,
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

            val remoteConfigManager = if (remoteConfigDiskCache != null) {
                val remoteConfigBlobStore = RemoteConfigBlobStore(contextForStorage)
                RemoteConfigManager(
                    backend = backend,
                    diskCache = remoteConfigDiskCache,
                    blobStore = remoteConfigBlobStore,
                    topicStore = remoteConfigTopicStore,
                    sourceProvider = apiSourceProvider,
                    blobFetcher = RemoteConfigBlobFetcher(
                        remoteConfigBlobStore,
                        apiSourceProvider,
                        timeoutManager,
                        urlConnectionFactory = blobUrlConnectionFactory(forceServerErrorStrategy),
                    ),
                    // Bootstrap source for a cold on-demand read's self-triggered sync (see blobData()); after
                    // the first identity change the manager syncs for the user clearCache() binds instead.
                    appUserIDProvider = { cache.getCachedAppUserID() },
                )
            } else {
                null
            }

            val fontLoader = FontLoader(
                context = contextForStorage,
            )
            val offeringFontPreDownloader = OfferingFontPreDownloader(
                context = contextForStorage,
                fontLoader = fontLoader,
            )

            // Single shared instances so the in-memory caches the render path reads synchronously are the same
            // ones the manager warms on commit. Registered as commit listeners; a null manager means workflows
            // are off, so neither exists.
            val uiConfigProvider = remoteConfigManager?.let { UiConfigProvider(it) }
            val workflowAssetPrewarmer = uiConfigProvider?.let {
                WorkflowAssetPrewarmer(it, paywallAssetWarming, offeringFontPreDownloader)
            }
            val workflowsConfigProvider = remoteConfigManager?.let {
                WorkflowsConfigProvider(
                    it,
                    currentOfferingIdProvider = { offeringsCache.cachedOfferings?.current?.identifier },
                    prewarmOfferingIdsProvider = {
                        offeringsCache.cachedOfferings?.prewarmTargetOfferingIds().orEmpty()
                    },
                    onWorkflowLoaded = workflowAssetPrewarmer?.let { it::onWorkflowLoaded },
                )
            }
            val checkpointsConfigProvider = remoteConfigManager?.let {
                CheckpointsConfigProvider(it)
            }
            val audiencesConfigProvider = remoteConfigManager?.let {
                AudiencesConfigProvider(it)
            }
            if (remoteConfigManager != null && uiConfigProvider != null && workflowsConfigProvider != null) {
                remoteConfigManager.registerListener(uiConfigProvider)
                remoteConfigManager.registerListener(workflowsConfigProvider)
                // Cold-start-with-warm-disk: preload the in-memory caches from whatever is already committed on
                // disk without triggering a network config sync. A subsequent network commit re-warms with a
                // higher generation and supersedes this (store-if-newer).
                val initialGeneration = remoteConfigManager.configGeneration
                uiConfigProvider.warmAsync(initialGeneration)
                workflowsConfigProvider.warmAsync(initialGeneration)
            }

            val identityManager = IdentityManager(
                appConfig,
                cache,
                subscriberAttributesCache,
                subscriberAttributesManager,
                offeringsCache,
                remoteConfigManager,
                backend,
                offlineEntitlementsManager,
                dispatcher,
                uiPreviewMode = appConfig.uiPreviewMode,
            )

            // Built after the identity manager so a dimension source reads the app user ID from this instance
            // rather than from whichever one is the singleton by the time a checkpoint is resolved. The evaluator
            // is a leaf, only consumed from there, so where it is built is otherwise unconstrained.
            RulesEngine.setLogger(RulesEngineLoggerBridge)
            val localRulesEvaluator = LocalRulesEvaluator(
                providers = listOf(
                    DeviceDimensionProvider(appConfig, localeProvider),
                    // Only read during a checkpoint evaluation, so the instance is configured by then. Same
                    // reasoning as CheckpointWorkflowResolverImpl's getOfferings.
                    StoreDimensionProvider { Purchases.sharedInstance.awaitStorefrontCountryCode() },
                    SubscriberAttributesDimensionProvider {
                        subscriberAttributesCache.getAllStoredSubscriberAttributes(
                            identityManager.currentAppUserID,
                        )
                    },
                ),
            )

            val customerInfoUpdateHandler = CustomerInfoUpdateHandler(
                cache,
                identityManager,
                offlineEntitlementsManager,
                appConfig = appConfig,
                diagnosticsTracker = diagnosticsTracker,
            )

            val paywallPresentedCache = PaywallPresentedCache()

            val localTransactionMetadataStore = LocalTransactionMetadataStore(contextForStorage, apiKey)

            val postReceiptHelper = PostReceiptHelper(
                appConfig,
                backend,
                billing,
                customerInfoUpdateHandler,
                cache,
                subscriberAttributesManager,
                offlineEntitlementsManager,
                paywallPresentedCache,
                localTransactionMetadataStore,
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
                postReceiptHelper,
            )

            val customerInfoHelper = CustomerInfoHelper(
                cache,
                backend,
                offlineEntitlementsManager,
                customerInfoUpdateHandler,
                postPendingTransactionsHelper,
                diagnosticsTracker,
                uiPreviewMode = appConfig.uiPreviewMode,
            )
            // Under workflows, paywall components are served from `/v1/config`, so skip capturing the raw
            // component JSON at parse time (memory). Reverts to decoding once the 4xx kill switch disables remote
            // config (or when workflows are off / customEntitlementComputation), so the fallback render path has
            // the components after a refetch. Evaluated per parse against the volatile `isDisabled`.
            val offeringParser = OfferingParserFactory.createOfferingParser(finalStore) {
                remoteConfigManager?.isDisabled ?: true
            }

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

            // Workflows are served from the `/v1/config` layer: WorkflowManager stays the consumer-facing seam,
            // but behind it sit the RemoteConfig stack (sync + blob store + on-demand fetch) and the
            // WorkflowsConfigProvider. Lifecycle (foreground refresh, identity clearCache, teardown) is driven
            // through remoteConfigManager, which the orchestrator and IdentityManager already own.
            // Both providers are non-null exactly when remoteConfigManager is (i.e. workflows are enabled).
            val workflowManager = if (workflowsConfigProvider != null && uiConfigProvider != null &&
                workflowAssetPrewarmer != null
            ) {
                WorkflowManager(
                    workflowsConfigProvider,
                    uiConfigProvider,
                    workflowAssetPrewarmer,
                )
            } else {
                null
            }

            val offeringsManager = OfferingsManager(
                offeringsCache,
                backend,
                OfferingsFactory(billing, offeringParser, dispatcher, appConfig),
                OfferingImagePreDownloader(assetWarming = paywallAssetWarming),
                diagnosticsTracker,
                offeringFontPreDownloader = offeringFontPreDownloader,
                offeringWebViewPrewarmer = OfferingWebViewPrewarmer(assetWarming = paywallAssetWarming),
                dispatcher = dispatcher,
                uiPreviewMode = appConfig.uiPreviewMode,
                workflowManager = workflowManager,
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

            val purchaseParamsValidator = PurchaseParamsValidator()

            val eventsManager = createEventsManager(
                identityManager,
                eventsDispatcher,
                backend,
                legacyEventsFileHelper = EventsManager.paywalls(fileHelper = FileHelper(application)),
                fileHelper = EventsManager.backendEvents(fileHelper = FileHelper(application)),
                baseURL = AppConfig.paywallEventsURL,
            )

            val adEventsManager = createEventsManager(
                identityManager,
                eventsDispatcher,
                backend,
                legacyEventsFileHelper = null,
                fileHelper = EventsManager.adEvents(fileHelper = FileHelper(application)),
                baseURL = AppConfig.adEventsURL,
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
                eventsManager = eventsManager,
                adEventsManager = adEventsManager,
                paywallPresentedCache = paywallPresentedCache,
                purchasesStateCache = purchasesStateProvider,
                dispatcher = dispatcher,
                initialConfiguration = configuration,
                fontLoader = fontLoader,
                localeProvider = localeProvider,
                virtualCurrencyManager = virtualCurrencyManager,
                purchaseParamsValidator = purchaseParamsValidator,
                workflowManager = workflowManager,
                fileRepository = fileRepository,
                remoteConfigManager = remoteConfigManager,
                uiConfigProvider = uiConfigProvider,
                workflowsConfigProvider = workflowsConfigProvider,
                checkpointsConfigProvider = checkpointsConfigProvider,
                audiencesConfigProvider = audiencesConfigProvider,
                localRulesEvaluator = localRulesEvaluator,
            )

            return Purchases(purchasesOrchestrator)
        }
    }

    @Suppress("LongParameterList")
    private fun createEventsManager(
        identityManager: IdentityManager,
        eventsDispatcher: Dispatcher,
        backend: Backend,
        legacyEventsFileHelper: EventsFileHelper<PaywallStoredEvent>?,
        fileHelper: EventsFileHelper<BackendStoredEvent>,
        baseURL: URL,
    ): EventsManager {
        return EventsManager(
            legacyEventsFileHelper = legacyEventsFileHelper,
            fileHelper = fileHelper,
            identityManager = identityManager,
            eventsDispatcher = eventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = baseURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
        )
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
                apiKeyValidationResult == APIKeyValidator.ValidationResult.SIMULATED_STORE &&
                !dangerousSettings.uiPreviewMode
            ) {
                val redactedApiKey = apiKeyValidator.redactApiKey(apiKey)
                errorLog(
                    error = PurchasesError(
                        code = PurchasesErrorCode.ConfigurationError,
                        underlyingErrorMessage = "Test Store API key used in release build: $redactedApiKey. " +
                            "Please configure the Play Store/Amazon app on the RevenueCat dashboard " +
                            "and use its corresponding API key before releasing. " +
                            "Visit https://rev.cat/sdk-test-store to learn more.",
                    ),
                )
                SimulatedStoreErrorDialogActivity.show(context, redactedApiKey)
                // SimulatedStoreErrorDialogActivity will crash the app when the user dismisses it.
                return apiKeyValidationResult
            }

            require(context.applicationContext is Application) { "Needs an application context." }

            return apiKeyValidationResult
        }
    }

    private fun blobUrlConnectionFactory(strategy: ForceServerErrorStrategy?): UrlConnectionFactory =
        DefaultUrlConnectionFactory().let { default ->
            strategy?.let { ForcedFailureUrlConnectionFactory(default, it) } ?: default
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

    private fun createRemoteConfigExecutor(): ExecutorService {
        return Executors.newSingleThreadScheduledExecutor()
    }

    private class LowPriorityThreadFactory(private val threadName: String) : ThreadFactory {
        override fun newThread(r: Runnable?): Thread {
            val wrapperRunnable = Runnable {
                r?.let {
                    android.os.Process.setThreadPriority(Thread.NORM_PRIORITY)
                    r.run()
                }
            }
            return Thread(wrapperRunnable, threadName)
        }
    }

    companion object {
        @VisibleForTesting
        internal fun shouldInitializeDiagnostics(
            diagnosticsEnabled: Boolean,
            uiPreviewMode: Boolean,
        ): Boolean = diagnosticsEnabled && !uiPreviewMode
    }
}
