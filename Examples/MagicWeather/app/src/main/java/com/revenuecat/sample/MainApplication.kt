package com.revenuecat.sample

import android.app.Application
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.sample.data.Constants
import com.revenuecat.sample.ui.user.UserViewModel

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /*
        Enable debug logs before calling `configure`.
        */
        Purchases.debugLogsEnabled = true

        /*
        Initialize the RevenueCat Purchases SDK.

        - appUserID is nil, so an anonymous ID will be generated automatically by the Purchases SDK. Read more about Identifying Users here: https://docs.revenuecat.com/docs/user-ids
        - observerMode is false, so Purchases will automatically handle finishing transactions. Read more about Observer Mode here: https://docs.revenuecat.com/docs/observer-mode
        */
        Purchases.configure(this, Constants.API_KEY, null, false)

        /*
        Whenever the `sharedInstance` of Purchases updates the PurchaserInfo cache, this method will be called.

        Note: PurchaserInfo is not pushed to each Purchases client, it has to be fetched.
        This list is only called when the SDK updates its cache after an app launch, purchase, restore, or fetch.
        You still need to call `Purchases.shared.purchaserInfo` to fetch PurchaserInfo regularly.
        */
        Purchases.sharedInstance.updatedPurchaserInfoListener = UpdatedPurchaserInfoListener {
            // - Update our user's purchaserInfo object
            UserViewModel.shared.purchaserInfo.value = it
        }
    }
}
