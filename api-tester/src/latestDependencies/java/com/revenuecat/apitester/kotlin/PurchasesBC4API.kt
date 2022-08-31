package com.revenuecat.apitester.kotlin

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.BillingFeature
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getNonSubscriptionSkusWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getSubscriptionSkusWith
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.restorePurchasesWith
import java.net.URL
import java.util.concurrent.ExecutorService

@Suppress("unused")
private class PurchasesBC4API {
    fun check(billingFeature: BillingFeature) {
        when (billingFeature) {
            BillingFeature.PRICE_CHANGE_CONFIRMATION,
            BillingFeature.SUBSCRIPTIONS,
            BillingFeature.SUBSCRIPTIONS_UPDATE,
            BillingFeature.IN_APP_ITEMS_ON_VR,
            BillingFeature.SUBSCRIPTIONS_ON_VR -> {}
        }.exhaustive
    }
}
