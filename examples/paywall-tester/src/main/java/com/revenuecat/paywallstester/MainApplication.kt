package com.revenuecat.paywallstester

import android.app.Application
import android.util.Log
import com.revenuecat.paywallstester.data.ApiKeyStore
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.DebugEventListener
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterListener

private const val TAG = "MainApplication"

class MainApplication : Application() {

    @OptIn(InternalRevenueCatAPI::class)
    override fun onCreate() {
        super.onCreate()

        Purchases.logLevel = LogLevel.VERBOSE

        val apiKey = ApiKeyStore(this).getLastUsedApiKey()
        val configurePurchases = ConfigurePurchasesUseCase(this)
        configurePurchases(apiKey)
        Purchases.sharedInstance.debugEventListener = DebugEventListener { event ->
            Log.d(TAG, "DebugEvent: ${event.name} ${event.properties}")
        }
        Purchases.sharedInstance.customerCenterListener =
            object : CustomerCenterListener {
                override fun onRestoreStarted() {
                    Log.d(TAG, "CustomerCenterListener: onRestoreStarted called")
                }

                override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                    Log.d(
                        TAG,
                        "CustomerCenterListener: onRestoreCompleted called with customer info: " +
                            customerInfo.originalAppUserId,
                    )
                }

                override fun onRestoreFailed(error: PurchasesError) {
                    Log.d(TAG, "CustomerCenterListener: onRestoreFailed called with error: ${error.message}")
                }

                override fun onShowingManageSubscriptions() {
                    Log.d(TAG, "CustomerCenterListener: onShowingManageSubscriptions called")
                }

                override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
                    Log.d(
                        TAG,
                        "CustomerCenterListener: onFeedbackSurveyCompleted called with option ID: " +
                            feedbackSurveyOptionId,
                    )
                }

                override fun onCustomActionSelected(actionIdentifier: String, purchaseIdentifier: String?) {
                    Log.d(
                        TAG,
                        "CustomerCenterListener: onCustomActionSelected called with action: $actionIdentifier, " +
                            "purchaseIdentifier: $purchaseIdentifier",
                    )
                }
            }
    }
}
