package com.revenuecat.purchases.integration.production.cachedofferings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.cachedofferings.BaseCachedOfferingsUsageIntegrationTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionCachedOfferingsUsageIntegrationTest : BaseCachedOfferingsUsageIntegrationTest() {
    override val environmentConfig get() = Constants.production
}
