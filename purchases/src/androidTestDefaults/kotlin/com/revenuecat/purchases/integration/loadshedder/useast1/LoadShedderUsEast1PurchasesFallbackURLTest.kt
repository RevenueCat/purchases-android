package com.revenuecat.purchases.integration.loadshedder.useast1

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.fallbackurl.PurchasesFallbackURLTest

class LoadShedderUsEast1PurchasesFallbackURLTest : PurchasesFallbackURLTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
