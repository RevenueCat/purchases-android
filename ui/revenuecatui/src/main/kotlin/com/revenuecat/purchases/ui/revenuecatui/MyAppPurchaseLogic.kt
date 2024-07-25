package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject


class MyAppPurchase(
    val purchase: Purchase,
    private val productType: ProductType = ProductType.UNKNOWN,
    private val presentedOfferingContext: PresentedOfferingContext? = null,
    private val subscriptionOptionId: String? = null,
    private val replacementMode: GoogleReplacementMode? = null
) {
    fun toStoreTransaction(): StoreTransaction = StoreTransaction(
        orderId = purchase.orderId,
        productIds = purchase.products,
        type = productType,
        purchaseTime = purchase.purchaseTime,
        purchaseToken = purchase.purchaseToken,
        purchaseState = purchase.purchaseState.toRevenueCatPurchaseState(),
        isAutoRenewing = purchase.isAutoRenewing,
        signature = purchase.signature,
        originalJson = JSONObject(purchase.originalJson),
        presentedOfferingContext = presentedOfferingContext,
        storeUserID = null,
        purchaseType = PurchaseType.GOOGLE_PURCHASE,
        marketplace = null,
        subscriptionOptionId = subscriptionOptionId,
        replacementMode = replacementMode,
    )

    private fun Int.toRevenueCatPurchaseState(): PurchaseState {
        return when (this) {
            Purchase.PurchaseState.UNSPECIFIED_STATE -> PurchaseState.UNSPECIFIED_STATE
            Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
            else -> PurchaseState.UNSPECIFIED_STATE
        }
    }
}

sealed class MyAppPurchaseResult {
    data class Success(val purchase: MyAppPurchase? = null) : MyAppPurchaseResult()
    object Cancellation : MyAppPurchaseResult()
    data class Error(val error: PurchasesError) : MyAppPurchaseResult()
}

sealed class MyAppRestoreResult {
    object Success : MyAppRestoreResult()
    data class Error(val error: PurchasesError) : MyAppRestoreResult()
}

class MyAppPurchaseLogic(
    val performPurchase: suspend ((Activity, Package) -> MyAppPurchaseResult),
    val performRestore: suspend ((CustomerInfo) -> MyAppRestoreResult),
)
