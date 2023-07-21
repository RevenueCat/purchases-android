package com.revenuecat.purchases.amazon

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration

class AmazonConfiguration(builder: Builder) : PurchasesConfiguration(builder) {

    class Builder(
        context: Context,
        apiKey: String,
        appUserId: String,
    ) : PurchasesConfiguration.Builder(context, apiKey, appUserId) {

        init {
            error("AmazonConfiguration is not currently supported in the custom entitlement computation package.")
        }
    }
}
