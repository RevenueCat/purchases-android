package com.revenuecat.purchases.integration.loadshedder.useast2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsFreshInstallInitialPurchasesTest
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsFreshInstallNoPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2OfflineEntitlementsFreshInstallNoPurchasesTest :
    BaseOfflineEntitlementsFreshInstallNoPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2OfflineEntitlementsFreshInstallInitialPurchasesTest :
    BaseOfflineEntitlementsFreshInstallInitialPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
