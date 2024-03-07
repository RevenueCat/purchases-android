package com.revenuecat.purchases

import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreTransaction

/**
 * This class will post store transactions after querying the product details to enrich the data.
 */
internal class PostTransactionWithProductDetailsHelper(
    private val billing: BillingAbstract,
    private val postReceiptHelper: PostReceiptHelper,
) {

    /**
     * The callbacks in this method are called for each transaction in the list.
     */
    @Suppress("LongParameterList")
    fun postTransactions(
        transactions: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        appUserID: String,
        initiationSource: PostReceiptInitiationSource,
        transactionPostSuccess: (SuccessfulPurchaseCallback)? = null,
        transactionPostError: (ErrorPurchaseCallback)? = null,
    ) {
        transactions.forEach { transaction ->
            if (transaction.purchaseState != PurchaseState.PENDING) {
                billing.queryProductDetailsAsync(
                    productType = transaction.type,
                    productIds = transaction.productIds.toSet(),
                    onReceive = { storeProducts ->
                        val purchasedStoreProduct =
                            // Amazon purchases don't have subscription options
                            // Amazon is the only store that has marketplace
                            // We want Google restores to enter in this if condition,
                            // that's why we need to filter Amazon only
                            if (transaction.type == ProductType.SUBS && transaction.marketplace == null) {
                                storeProducts.firstOrNull { product ->
                                    product.subscriptionOptions?.let { subscriptionOptions ->
                                        subscriptionOptions.any { it.id == transaction.subscriptionOptionId }
                                    } ?: false
                                }
                            } else {
                                storeProducts.firstOrNull { product ->
                                    product.id == transaction.productIds.firstOrNull()
                                }
                            }
                        debugLog("Store product found for transaction: $purchasedStoreProduct")
                        postReceiptHelper.postTransactionAndConsumeIfNeeded(
                            purchase = transaction,
                            storeProduct = purchasedStoreProduct,
                            isRestore = allowSharingPlayStoreAccount,
                            appUserID = appUserID,
                            initiationSource = initiationSource,
                            onSuccess = transactionPostSuccess,
                            onError = transactionPostError,
                        )
                    },
                    onError = {
                        postReceiptHelper.postTransactionAndConsumeIfNeeded(
                            purchase = transaction,
                            storeProduct = null,
                            isRestore = allowSharingPlayStoreAccount,
                            appUserID = appUserID,
                            initiationSource = initiationSource,
                            onSuccess = transactionPostSuccess,
                            onError = transactionPostError,
                        )
                    },
                )
            } else {
                transactionPostError?.invoke(
                    transaction,
                    PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) },
                )
            }
        }
    }
}
