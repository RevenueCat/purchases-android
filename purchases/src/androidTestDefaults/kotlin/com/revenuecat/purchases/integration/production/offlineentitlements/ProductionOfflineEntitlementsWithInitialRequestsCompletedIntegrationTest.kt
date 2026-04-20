package com.revenuecat.purchases.integration.production.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionOfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest :
    BaseOfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest() {
    override val environmentConfig get() = Constants.production
}

@RunWith(AndroidJUnit4::class)
class ProductionOfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest :
    BaseOfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest() {
    override val environmentConfig get() = Constants.production
}
