package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Result of the paywall activity.
 */
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
    @Poko
    class Purchased(val customerInfo: CustomerInfo) : PaywallResult(), Parcelable

    /**
     * The user tried to purchase a product or restore purchases but an error occurred. If they tried multiple times,
     * the error corresponds to the last attempt.
     */
    @Parcelize
    @Poko
    class Error(val error: PurchasesError) : PaywallResult(), Parcelable

    /**
     * The last action the user performed in the paywall activity was a restore.
     */
    @Parcelize
    @Poko
    class Restored(val customerInfo: CustomerInfo) : PaywallResult(), Parcelable
}
