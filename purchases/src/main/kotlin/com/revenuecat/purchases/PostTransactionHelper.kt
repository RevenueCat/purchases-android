package com.revenuecat.purchases

import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreTransaction

internal class PostTransactionHelper(
    private val billing: BillingAbstract,
    private val postReceiptHelper: PostReceiptHelper,
) {

    /**
     * The callbacks in this method are called for each transaction in the list.
     */
    fun postTransactions(
        transactions: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        appUserID: String,
        transactionPostSuccess: (SuccessfulPurchaseCallback)? = null,
        transactionPostError: (ErrorPurchaseCallback)? = null,
    ) {
        transactions.forEach { transaction ->
            if (transaction.purchaseState != PurchaseState.PENDING) {
                billing.queryProductDetailsAsync(
                    productType = transaction.type,
                    productIds = transaction.productIds.toSet(),
                    onReceive = { storeProducts ->
                        val purchasedStoreProduct = if (transaction.type == ProductType.SUBS) {
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

                        postReceiptHelper.postTransactionAndConsumeIfNeeded(
                            purchase = transaction,
                            storeProduct = purchasedStoreProduct,
                            isRestore = allowSharingPlayStoreAccount,
                            appUserID = appUserID,
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
