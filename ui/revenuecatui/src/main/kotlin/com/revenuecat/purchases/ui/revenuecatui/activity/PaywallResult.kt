package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import kotlinx.parcelize.Parcelize

sealed class PaywallResult: Parcelable {
    @Parcelize
    object Cancelled: PaywallResult(), Parcelable
    @Parcelize
    data class Purchased(val customerInfo: CustomerInfo): PaywallResult(), Parcelable
    @Parcelize
    data class Error(val error: PurchasesError): PaywallResult(), Parcelable
}
