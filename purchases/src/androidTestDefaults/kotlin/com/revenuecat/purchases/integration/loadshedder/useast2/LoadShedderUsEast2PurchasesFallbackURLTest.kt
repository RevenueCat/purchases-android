package com.revenuecat.purchases.integration.loadshedder.useast2

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.fallbackurl.PurchasesFallbackURLTest

class LoadShedderUsEast2PurchasesFallbackURLTest : PurchasesFallbackURLTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
