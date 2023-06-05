package com.revenuecat.purchases.amazon

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.Store

class AmazonConfiguration(builder: Builder) : PurchasesConfiguration(builder) {

    class Builder(
        context: Context,
        apiKey: String,
    ) : PurchasesConfiguration.Builder(context, apiKey) {

        init {
            this.store(Store.AMAZON)
        }
    }
}
