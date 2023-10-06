package com.revenuecat.sample

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.sample.data.Constants
import com.revenuecat.sample.ui.user.UserViewModel
import java.lang.IllegalArgumentException

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /*
        Enable debug logs before calling `configure`.
         */
        Purchases.logLevel = LogLevel.DEBUG

        /*
        Initialize the RevenueCat Purchases SDK.

        - appUserID is nil, so an anonymous ID will be generated automatically by the Purchases SDK. Read more about Identifying Users here: https://docs.revenuecat.com/docs/user-ids
        - observerMode is false, so Purchases will automatically handle finishing transactions. Read more about Observer Mode here: https://docs.revenuecat.com/docs/observer-mode
         */
        val builder = when (BuildConfig.STORE) {
            "amazon" -> AmazonConfiguration.Builder(this, Constants.AMAZON_API_KEY)
            "google" -> PurchasesConfiguration.Builder(this, Constants.GOOGLE_API_KEY)
            else -> throw IllegalArgumentException("Invalid store.")
        }
        Purchases.configure(
            builder
                .observerMode(false)
                .appUserID(null)
                .build(),
        )

        /*
        Whenever the `sharedInstance` of Purchases updates the CustomerInfo cache, this method will be called.

        Note: CustomerInfo is not pushed to each Purchases client, it has to be fetched.
        This list is only called when the SDK updates its cache after an app launch, purchase, restore, or fetch.
        You still need to call `Purchases.shared.customerInfo` to fetch CustomerInfo regularly.
         */
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
            // - Update our user's customerInfo object
            UserViewModel.shared.customerInfo.value = it
        }
    }
}
