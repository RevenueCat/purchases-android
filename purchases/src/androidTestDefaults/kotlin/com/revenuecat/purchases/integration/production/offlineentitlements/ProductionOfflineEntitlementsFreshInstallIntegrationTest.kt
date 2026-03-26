package com.revenuecat.purchases.integration.production.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsFreshInstallInitialPurchasesTest
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsFreshInstallNoPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionOfflineEntitlementsFreshInstallNoPurchasesTest :
    BaseOfflineEntitlementsFreshInstallNoPurchasesTest() {
    override val environmentConfig get() = Constants.production
}

@RunWith(AndroidJUnit4::class)
class ProductionOfflineEntitlementsFreshInstallInitialPurchasesTest :
    BaseOfflineEntitlementsFreshInstallInitialPurchasesTest() {
    override val environmentConfig get() = Constants.production
}
