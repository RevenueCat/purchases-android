package com.revenuecat.purchases

import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings

internal class SyncPurchasesHelper(
    private val billing: BillingAbstract,
    private val identityManager: IdentityManager,
    private val postReceiptHelper: PostReceiptHelper
) {
    fun syncPurchases(
        isRestore: Boolean,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        log(LogIntent.DEBUG, PurchaseStrings.SYNCING_PURCHASES)

        val appUserID = identityManager.currentAppUserID

        billing.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = { allPurchases ->
                if (allPurchases.isNotEmpty()) {
                    val lastPurchase = allPurchases.last()
                    val errors: MutableList<PurchasesError> = mutableListOf()
                    fun handleLastPurchase(currentPurchase: StoreTransaction, lastPurchase: StoreTransaction) {
                        if (currentPurchase == lastPurchase) {
                            if (errors.isEmpty()) {
                                debugLog(PurchaseStrings.SYNCED_PURCHASES_SUCCESSFULLY)
                                onSuccess()
                            } else {
                                errorLog(PurchaseStrings.SYNCING_PURCHASES_ERROR.format(errors))
                                onError(errors.first())
                            }
                        }
                    }
                    allPurchases.forEach { purchase ->
                        val productInfo = ReceiptInfo(productIDs = purchase.productIds)
                        postReceiptHelper.postTokenWithoutConsuming(
                            purchase.purchaseToken,
                            purchase.storeUserID,
                            productInfo,
                            isRestore,
                            appUserID,
                            purchase.marketplace,
                            {
                                log(LogIntent.PURCHASE, PurchaseStrings.PURCHASE_SYNCED.format(purchase))
                                handleLastPurchase(purchase, lastPurchase)
                            },
                            { error ->
                                log(
                                    LogIntent.RC_ERROR, PurchaseStrings.SYNCING_PURCHASES_ERROR_DETAILS
                                        .format(purchase, error)
                                )
                                errors.add(error)
                                handleLastPurchase(purchase, lastPurchase)
                            }
                        )
                    }
                } else {
                    onSuccess()
                }
            },
            onReceivePurchaseHistoryError = {
                log(LogIntent.RC_ERROR, PurchaseStrings.SYNCING_PURCHASES_ERROR.format(it))
                onError(it)
            }
        )
    }
}
