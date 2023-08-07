package com.revenuecat.purchases.ui.debugview

import android.app.Activity
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.debugview.models.SettingScreenState
import kotlinx.coroutines.flow.StateFlow

internal interface DebugRevenueCatViewModel {
    val state: StateFlow<SettingScreenState>

    fun toastDisplayed()
    fun purchasePackage(activity: Activity, rcPackage: Package)
    fun purchaseProduct(activity: Activity, storeProduct: StoreProduct)
    fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption)
}
