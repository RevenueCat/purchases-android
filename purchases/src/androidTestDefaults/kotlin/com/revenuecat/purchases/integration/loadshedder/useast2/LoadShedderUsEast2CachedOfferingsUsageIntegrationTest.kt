package com.revenuecat.purchases.integration.loadshedder.useast2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.cachedofferings.BaseCachedOfferingsUsageIntegrationTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2CachedOfferingsUsageIntegrationTest : BaseCachedOfferingsUsageIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
