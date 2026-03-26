package com.revenuecat.purchases.integration.loadshedder.useast1

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.PurchasesIntegrationTest

class LoadShedderUsEast1PurchasesIntegrationTest : PurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
