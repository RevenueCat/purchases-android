package com.revenuecat.purchases.galaxy

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.SharedConstants
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.galaxy.constants.GalaxyConsumeOrAcknowledgeStatusCode
import com.revenuecat.purchases.galaxy.conversions.toSamsungIAPOperationMode
import com.revenuecat.purchases.galaxy.conversions.toStoreTransaction
import com.revenuecat.purchases.galaxy.handler.AcknowledgePurchaseHandler
import com.revenuecat.purchases.galaxy.handler.ChangeSubscriptionPlanHandler
import com.revenuecat.purchases.galaxy.handler.GetOwnedListHandler
import com.revenuecat.purchases.galaxy.handler.ProductDataHandler
import com.revenuecat.purchases.galaxy.handler.PurchaseHandler
import com.revenuecat.purchases.galaxy.listener.AcknowledgePurchaseResponseListener
import com.revenuecat.purchases.galaxy.listener.ChangeSubscriptionPlanResponseListener
import com.revenuecat.purchases.galaxy.listener.GetOwnedListResponseListener
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import com.revenuecat.purchases.galaxy.listener.PurchaseResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.SerialRequestExecutor
import com.revenuecat.purchases.galaxy.utils.parseDateFromGalaxyDateString
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.samsung.android.sdk.iap.lib.constants.HelperConstants
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

@OptIn(InternalRevenueCatAPI::class)
@Suppress("TooManyFunctions", "LongParameterList")
internal class GalaxyBillingWrapper(
    stateProvider: PurchasesStateProvider,
    private val context: Context,
    private val deviceCache: DeviceCache,
    val billingMode: GalaxyBillingMode,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val iapHelper: IAPHelperProvider = DefaultIAPHelperProvider(
        iapHelper = IapHelper.getInstance(context),
    ),
    private val productDataHandler: ProductDataResponseListener =
        ProductDataHandler(
            iapHelper = iapHelper,
        ),
    private val purchaseHandler: PurchaseResponseListener =
        PurchaseHandler(
            iapHelper = iapHelper,
        ),
    private val acknowledgePurchaseHandler: AcknowledgePurchaseResponseListener =
        AcknowledgePurchaseHandler(
            iapHelper = iapHelper,
            context = context,
        ),
    private val getOwnedListHandler: GetOwnedListResponseListener =
        GetOwnedListHandler(
            iapHelper = iapHelper,
        ),
    private val changeSubscriptionPlanHandler: ChangeSubscriptionPlanResponseListener =
        ChangeSubscriptionPlanHandler(
            iapHelper = iapHelper,
        ),
) : BillingAbstract(purchasesStateProvider = stateProvider) {

    constructor(
        stateProvider: PurchasesStateProvider,
        context: Context,
        billingMode: GalaxyBillingMode,
        deviceCache: DeviceCache,
    ) : this(
        stateProvider = stateProvider,
        context = context,
        deviceCache = deviceCache,
        billingMode = billingMode,
        dateProvider = DefaultDateProvider(),
    )

    private val serialRequestExecutor = SerialRequestExecutor()

    init {
        iapHelper.setOperationMode(mode = billingMode.toSamsungIAPOperationMode())
        logWarningIfUnexpectedSamsungIAPVersionFound()
    }

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        // No-op
    }

    override fun startConnection() {
        // No-op
    }

    override fun endConnection() {
        // No-op
    }

    @OptIn(GalaxySerialOperation::class)
    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        serialRequestExecutor.executeSerially { finish ->
            getOwnedListHandler.getOwnedList(
                onSuccess = { ownedProducts ->
                    val storeTransactions = ownedProducts.map {
                        try {
                            it.toStoreTransaction(purchaseState = PurchaseState.PURCHASED)
                        } catch (e: IllegalArgumentException) {
                            val errorMessage = GalaxyStrings.ERROR_CANNOT_PARSE_PURCHASE_RESULT.format(e.message)
                            log(LogIntent.GALAXY_ERROR) { errorMessage }

                            val error = PurchasesError(
                                code = PurchasesErrorCode.InvalidReceiptError,
                                underlyingErrorMessage = errorMessage,
                            )
                            onReceivePurchaseHistoryError(error)
                            finish()
                            return@getOwnedList
                        }
                    }

                    onReceivePurchaseHistory(storeTransactions)
                    finish()
                },
                onError = { error ->
                    onReceivePurchaseHistoryError(error)
                    finish()
                },
            )
        }
    }

    @OptIn(GalaxySerialOperation::class)
    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        if (purchasesUpdatedListener == null) return

        serialRequestExecutor.executeSerially { finish ->
            @Suppress("ForbiddenComment")
            // TODO: Record diagnostics
            productDataHandler.getProductDetails(
                productIds = productIds,
                productType = productType,
                onReceive = {
                    onReceive(it)
                    finish()
                },
                onError = {
                    onError(it)
                    finish()
                },
            )
        }
    }

    override fun consumeAndSave(
        finishTransactions: Boolean,
        purchase: StoreTransaction,
        shouldConsume: Boolean,
        initiationSource: PostReceiptInitiationSource,
    ) {
        if (!finishTransactions || purchase.type == ProductType.UNKNOWN) {
            deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
            return
        }

        // PENDING purchases should not be fulfilled
        if (purchase.purchaseState == PurchaseState.PENDING) return

        if (purchase.type == ProductType.SUBS) {
            acknowledgePurchase(
                storeTransaction = purchase,
                onAcknowledged = deviceCache::addSuccessfullyPostedToken,
            )
        } else {
            log(LogIntent.GALAXY_WARNING) { GalaxyStrings.WARNING_CANNOT_CONSUME_NON_SUBS_PRODUCT_TYPES }
        }
    }

    @OptIn(GalaxySerialOperation::class)
    internal fun acknowledgePurchase(
        storeTransaction: StoreTransaction,
        onAcknowledged: (purchaseToken: String) -> Unit,
    ) {
        serialRequestExecutor.executeSerially { finish ->
            getOwnedListHandler.getOwnedList(
                onSuccess = { ownedProducts ->
                    val productIdToAcknowledge = storeTransaction.productIds.firstOrNull()
                    if (productIdToAcknowledge == null) {
                        finish()
                        return@getOwnedList
                    }

                    val purchaseHasBeenAcknowledgedAlready: Boolean =
                        ownedProducts.firstOrNull { it.itemId == productIdToAcknowledge }
                            ?.acknowledgedStatus == HelperDefine.AcknowledgedStatus.ACKNOWLEDGED

                    if (!purchaseHasBeenAcknowledgedAlready) {
                        acknowledgePurchaseHandler.acknowledgePurchase(
                            transaction = storeTransaction,
                            onSuccess = { acknowledgementResult ->
                                val resultStatus = GalaxyConsumeOrAcknowledgeStatusCode.fromCode(
                                    code = acknowledgementResult.statusCode,
                                )

                                if (resultStatus == null) {
                                    log(LogIntent.GALAXY_ERROR) {
                                        GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_UNKNOWN_STATUS_CODE
                                            .format(acknowledgementResult.statusCode)
                                    }
                                } else if (resultStatus != GalaxyConsumeOrAcknowledgeStatusCode.SUCCESS) {
                                    log(LogIntent.GALAXY_ERROR) {
                                        GalaxyStrings.ACKNOWLEDGE_REQUEST_RETURNED_ERROR_STATUS_CODE
                                            .format(
                                                acknowledgementResult.statusCode,
                                                acknowledgementResult.statusString,
                                            )
                                    }
                                } else {
                                    onAcknowledged(storeTransaction.purchaseToken)
                                }

                                finish()
                            },
                            onError = { _ -> finish() },
                        )
                    } else {
                        log(LogIntent.DEBUG) {
                            GalaxyStrings.NOT_ACKNOWLEDGING_TRANSACTION_BECAUSE_ALREADY_ACKNOWLEDGED
                                .format(storeTransaction.productIds.firstOrNull() ?: "none")
                        }
                        finish()
                    }
                },
                onError = { _ ->
                    finish()
                },
            )
        }
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG) { RestoreStrings.QUERYING_PURCHASE_WITH_TYPE.format(productId, productType.name) }
        queryAllPurchases(
            appUserID = appUserID,
            onReceivePurchaseHistory = { storeTransactions ->
                val matchingTransaction: StoreTransaction? = storeTransactions.firstOrNull { storeTransaction ->
                    productId == storeTransaction.productIds.firstOrNull()
                }
                if (matchingTransaction != null) {
                    onCompletion(matchingTransaction)
                } else {
                    val message = PurchaseStrings.NO_EXISTING_PURCHASE.format(productId)
                    val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)
                    onError(error)
                }
            },
            onReceivePurchaseHistoryError = onError,
        )
    }

    @Suppress("ReturnCount", "LongMethod")
    @OptIn(GalaxySerialOperation::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        val galaxyPurchaseInfo = purchasingData as? GalaxyPurchasingData.Product
        if (galaxyPurchaseInfo == null) {
            val errorMessage = PurchaseStrings.INVALID_PURCHASE_TYPE.format(
                "Galaxy",
                "GalaxyPurchasingData",
            )
            val error = PurchasesError(
                PurchasesErrorCode.UnknownError,
                errorMessage,
            )
            log(LogIntent.GALAXY_ERROR) { errorMessage }
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }
        if (galaxyPurchaseInfo.productType == ProductType.INAPP) {
            val error = PurchasesError(
                PurchasesErrorCode.UnsupportedError,
                GalaxyStrings.GALAXY_OTPS_NOT_SUPPORTED,
            )
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_OTPS_NOT_SUPPORTED }
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
            return
        }

        val productId = galaxyPurchaseInfo.productId

        if (replaceProductInfo != null) {
            val galaxyReplacementMode = replaceProductInfo.replacementMode as? GalaxyReplacementMode
                ?: GalaxyReplacementMode.default

            serialRequestExecutor.executeSerially { finish ->
                changeSubscriptionPlanHandler.changeSubscriptionPlan(
                    appUserID = appUserID,
                    oldPurchase = replaceProductInfo.oldPurchase,
                    newProductId = productId,
                    prorationMode = galaxyReplacementMode,
                    onSuccess = { receipt ->
                        handleReceipt(
                            receipt = receipt,
                            productId = productId,
                            presentedOfferingContext = presentedOfferingContext,
                            replacementMode = galaxyReplacementMode,
                        )
                        finish()
                    },
                    onError = { purchasesError ->
                        onPurchaseError(error = purchasesError)
                        finish()
                    },
                )
            }
            return
        }

        serialRequestExecutor.executeSerially { finish ->
            purchaseHandler.purchase(
                appUserID = appUserID,
                productId = productId,
                onSuccess = { receipt ->
                    handleReceipt(
                        receipt = receipt,
                        productId = productId,
                        presentedOfferingContext = presentedOfferingContext,
                        replacementMode = null,
                    )
                    finish()
                },
                onError = { purchasesError ->
                    onPurchaseError(error = purchasesError)
                    finish()
                },
            )
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun handleReceipt(
        receipt: PurchaseVo,
        productId: String,
        presentedOfferingContext: PresentedOfferingContext?,
        replacementMode: GalaxyReplacementMode?,
    ) {
        try {
            val storeTransaction = receipt.toStoreTransaction(
                productId = productId,
                presentedOfferingContext = presentedOfferingContext,
                purchaseState = PurchaseState.PURCHASED,
                replacementMode = replacementMode,
            )
            purchasesUpdatedListener?.onPurchasesUpdated(purchases = listOf(storeTransaction))
        } catch (e: IllegalArgumentException) {
            val errorMessage = GalaxyStrings.ERROR_CANNOT_PARSE_PURCHASE_RESULT.format(e.message)
            log(LogIntent.GALAXY_ERROR) { errorMessage }

            val error = PurchasesError(
                code = PurchasesErrorCode.InvalidReceiptError,
                underlyingErrorMessage = errorMessage,
            )
            purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
        }
    }

    override fun isConnected(): Boolean = true

    @OptIn(GalaxySerialOperation::class)
    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.DEBUG) { RestoreStrings.QUERYING_PURCHASE }
        serialRequestExecutor.executeSerially { finish ->
            getOwnedListHandler.getOwnedList(
                onSuccess = { ownedProducts ->
                    val storeTransactions = ownedProducts
                        .filter {
                            // TO DO: Find out what this returns for OTPs when we support OTPs
                            it.subscriptionEndDate.parseDateFromGalaxyDateString() > dateProvider.now
                        }
                        .map {
                            try {
                                it.toStoreTransaction(purchaseState = PurchaseState.PURCHASED)
                            } catch (e: IllegalArgumentException) {
                                val errorMessage = GalaxyStrings.ERROR_CANNOT_PARSE_PURCHASE_RESULT.format(e.message)
                                log(LogIntent.GALAXY_ERROR) { errorMessage }

                                val error = PurchasesError(
                                    code = PurchasesErrorCode.InvalidReceiptError,
                                    underlyingErrorMessage = errorMessage,
                                )
                                onError(error)
                                finish()
                                return@getOwnedList
                            }
                        }

                    val purchasesMap = storeTransactions.associateBy { storeTransaction ->
                        storeTransaction.purchaseToken.sha1()
                    }
                    onSuccess(purchasesMap)
                    finish()
                },
                onError = { error ->
                    onError(error)
                    finish()
                },
            )
        }
    }

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) {
        // No-op: Galaxy Store doesn't have in-app messages
    }

    override fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        log(LogIntent.GALAXY_ERROR) { GalaxyStrings.STOREFRONT_NOT_SUPPORTED }
        onError(
            PurchasesError(
                code = PurchasesErrorCode.UnsupportedError,
                underlyingErrorMessage = GalaxyStrings.STOREFRONT_NOT_SUPPORTED,
            ),
        )
    }

    private fun onPurchaseError(error: PurchasesError) {
        purchasesUpdatedListener?.onPurchasesFailedToUpdate(error)
    }

    private fun logWarningIfUnexpectedSamsungIAPVersionFound() {
        if (HelperConstants.HELPER_VERSION != SharedConstants.EXPECTED_SAMSUNG_IAP_SDK_VERSION) {
            log(intent = LogIntent.GALAXY_WARNING) {
                "Unexpected Samsung IAP SDK version found. Expected " +
                    "${SharedConstants.EXPECTED_SAMSUNG_IAP_SDK_VERSION}, but found ${HelperConstants.HELPER_VERSION}" +
                    ". Unexpected behavior related to the Galaxy Store in-app purchases may occur. Please obtain and" +
                    " include version ${HelperConstants.HELPER_VERSION} of the Samsung IAP SDK from the Samsung's" +
                    " developer website."
            }
        }
    }
}
