package com.revenuecat.purchases.integration.loadshedder.useast2

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.PurchasesIntegrationTest

class LoadShedderUsEast2PurchasesIntegrationTest : PurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
