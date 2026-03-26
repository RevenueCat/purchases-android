package com.revenuecat.purchases.integration.loadshedder.useast2

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest

class LoadShedderUsEast2OfflineEntitlementsFreshInstallNoPurchasesTest :
    OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}

class LoadShedderUsEast2OfflineEntitlementsFreshInstallInitialPurchasesTest :
    OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
