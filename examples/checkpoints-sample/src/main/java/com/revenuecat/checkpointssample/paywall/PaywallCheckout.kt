package com.revenuecat.checkpointssample.paywall

import android.app.Activity
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore

sealed interface CheckoutOutcome {
    object Completed : CheckoutOutcome
    object Cancelled : CheckoutOutcome
    class Failed(val message: String) : CheckoutOutcome
}

object PaywallCheckout {

    suspend fun purchase(activity: Activity, packageToPurchase: Package): CheckoutOutcome = try {
        Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, packageToPurchase).build())
        CheckoutOutcome.Completed
    } catch (e: PurchasesException) {
        if (e.code == PurchasesErrorCode.PurchaseCancelledError) {
            CheckoutOutcome.Cancelled
        } else {
            CheckoutOutcome.Failed("Purchase failed: ${e.message}")
        }
    }

    suspend fun restore(): CheckoutOutcome = try {
        val customerInfo = Purchases.sharedInstance.awaitRestore()
        if (customerInfo.entitlements.active.isNotEmpty()) {
            CheckoutOutcome.Completed
        } else {
            CheckoutOutcome.Failed("Nothing to restore.")
        }
    } catch (e: PurchasesException) {
        CheckoutOutcome.Failed("Restore failed: ${e.message}")
    }
}
