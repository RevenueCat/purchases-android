package com.revenuecat.purchases.integration.loadshedder.useast1

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.cachedofferings.CachedOfferingsUsageIntegrationTest

class LoadShedderUsEast1CachedOfferingsUsageIntegrationTest : CachedOfferingsUsageIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
