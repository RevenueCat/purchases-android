package com.revenuecat.purchases.integration.loadshedder.useast2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2OfflineEntitlementsFreshInstallNoPurchasesTest :
    OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2OfflineEntitlementsFreshInstallInitialPurchasesTest :
    OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
