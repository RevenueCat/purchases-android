package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import kotlinx.parcelize.Parcelize

/**
 * Result of the paywall activity.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
sealed class PaywallResult : Parcelable {
    /**
     * The user cancelled the paywall without purchasing.
     */
    @Parcelize
    object Cancelled : PaywallResult(), Parcelable

    /**
     * The user purchased a product and the paywall was closed.
     */
    @Parcelize
    data class Purchased(val customerInfo: CustomerInfo) : PaywallResult(), Parcelable

    /**
     * The user tried to purchase a product but an error occurred. If they tried multiple times,
     * the error corresponds to the last attempt.
     */
    @Parcelize
    data class Error(val error: PurchasesError) : PaywallResult(), Parcelable

    /**
     * The last action the user performed in the paywall activity was a restore.
     */
    @Parcelize
    data class Restored(val customerInfo: CustomerInfo) : PaywallResult(), Parcelable
}
