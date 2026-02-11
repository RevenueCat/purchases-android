package com.revenuecat.purchases

import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.models.GoogleStoreProduct
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
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Suppress("LongParameterList", "LongMethod")
    public fun postTransactions(
        transactions: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        appUserID: String,
        initiationSource: PostReceiptInitiationSource,
        sdkOriginated: Boolean,
        transactionPostSuccess: (SuccessfulPurchaseCallback)? = null,
        transactionPostError: (ErrorPurchaseCallback)? = null,
    ) {
        transactions.forEach { transaction ->
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

                    val subscriptionOptionsForGoogleProductIDs = transaction
                        .subscriptionOptionIdForProductIDs?.let { subscriptionOptionIds ->
                            // Build a map in the format "productID_subscriptionOptionID" for all
                            // product/subscription option combos
                            val allSubscriptionOptionsMap = storeProducts
                                .filterIsInstance<GoogleStoreProduct>()
                                .flatMap { product ->
                                    product.subscriptionOptions?.map { option ->
                                        "${product.productId}_${option.id}" to option
                                    } ?: emptyList()
                                }
                                .toMap()

                            // Then, return back only the productID/subscription option combinations
                            // that are in the transaction.
                            buildMap {
                                subscriptionOptionIds.forEach { (productId, subscriptionOptionId) ->
                                    allSubscriptionOptionsMap["${productId}_$subscriptionOptionId"]
                                        ?.let { subscriptionOption ->
                                            put(productId, subscriptionOption)
                                        }
                                }
                            }
                        }

                    debugLog { "Store product found for transaction: $purchasedStoreProduct" }
                    postReceiptHelper.postTransactionAndConsumeIfNeeded(
                        purchase = transaction,
                        storeProduct = purchasedStoreProduct,
                        subscriptionOptionForProductIDs = subscriptionOptionsForGoogleProductIDs,
                        isRestore = allowSharingPlayStoreAccount,
                        appUserID = appUserID,
                        initiationSource = initiationSource,
                        sdkOriginated = sdkOriginated,
                        onSuccess = transactionPostSuccess,
                        onError = transactionPostError,
                    )
                },
                onError = {
                    postReceiptHelper.postTransactionAndConsumeIfNeeded(
                        purchase = transaction,
                        storeProduct = null,
                        subscriptionOptionForProductIDs = null,
                        isRestore = allowSharingPlayStoreAccount,
                        appUserID = appUserID,
                        initiationSource = initiationSource,
                        sdkOriginated = sdkOriginated,
                        onSuccess = transactionPostSuccess,
                        onError = transactionPostError,
                    )
                },
            )
        }
    }
}
