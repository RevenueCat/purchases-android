package com.revenuecat.purchases.amazon

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration

// TODO: make public
internal class AmazonConfiguration(builder: Builder) : PurchasesConfiguration(builder) {

    init {
        // TODO: uncomment
        // this.store = Store.AMAZON
    }

    class Builder(
        context: Context,
        apiKey: String
    ) : PurchasesConfiguration.Builder(context, apiKey)
}
