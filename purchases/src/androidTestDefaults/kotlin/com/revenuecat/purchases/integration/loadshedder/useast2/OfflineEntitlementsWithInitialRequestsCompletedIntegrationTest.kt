package com.revenuecat.purchases.integration.loadshedder.useast2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsWithInitialRequestsCompletedAndInitialPurchasesIntegrationTest
import com.revenuecat.purchases.integration.production.offlineentitlements.OfflineEntitlementsWithInitialRequestsCompletedAndNoInitialPurchasesIntegrationTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2OfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest :
    OfflineEntitlementsWithInitialRequestsCompletedAndInitialPurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2OfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest :
    OfflineEntitlementsWithInitialRequestsCompletedAndNoInitialPurchasesIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
