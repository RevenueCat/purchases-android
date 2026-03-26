package com.revenuecat.purchases.integration.loadshedder.useast1

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest

class LoadShedderUsEast1OfflineEntitlementsFreshInstallNoPurchasesTest :
    OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}

class LoadShedderUsEast1OfflineEntitlementsFreshInstallInitialPurchasesTest :
    OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
