package com.revenuecat.purchases.integration.loadshedder.useast2

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.cachedofferings.CachedOfferingsUsageIntegrationTest

class LoadShedderUsEast2CachedOfferingsUsageIntegrationTest : CachedOfferingsUsageIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
