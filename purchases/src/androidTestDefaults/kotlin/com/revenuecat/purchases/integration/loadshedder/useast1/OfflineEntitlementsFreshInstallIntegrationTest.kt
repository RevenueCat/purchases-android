package com.revenuecat.purchases.integration.loadshedder.useast1

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast1OfflineEntitlementsFreshInstallNoPurchasesTest :
    OfflineEntitlementsFreshInstallIntegrationNoPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast1OfflineEntitlementsFreshInstallInitialPurchasesTest :
    OfflineEntitlementsFreshInstallInitialPurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
