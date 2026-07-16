package com.revenuecat.e2etests

import android.app.Application
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class E2ETestsApplication : Application() {

    @OptIn(InternalRevenueCatAPI::class)
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.DEBUG

        // The workflow E2E flows are built with E2E_WORKFLOWS_API_KEY set (surfaced as
        // BuildConfig.WORKFLOWS_API_KEY). When it's present we point at that project and enable
        // workflows; otherwise we keep the default configuration used by the CI
        // test_store_annual_purchase flow untouched. The Maestro launch argument only selects the
        // screen (in MainActivity), so configuration stays here in the Application.
        val builder = if (BuildConfig.WORKFLOWS_API_KEY != WORKFLOWS_API_KEY_PLACEHOLDER) {
            PurchasesConfiguration.Builder(context = this, apiKey = BuildConfig.WORKFLOWS_API_KEY)
                .dangerousSettings(DangerousSettings.forWorkflows())
        } else {
            PurchasesConfiguration.Builder(context = this, apiKey = Constants.API_KEY)
        }

        Purchases.configure(builder.build())
    }

    private companion object {
        const val WORKFLOWS_API_KEY_PLACEHOLDER = "workflows_api_key_to_replace"
    }
}
