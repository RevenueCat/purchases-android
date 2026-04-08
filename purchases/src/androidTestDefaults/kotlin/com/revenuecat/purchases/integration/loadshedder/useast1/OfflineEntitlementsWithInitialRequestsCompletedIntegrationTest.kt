package com.revenuecat.purchases.integration.loadshedder.useast1

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest
import com.revenuecat.purchases.integration.offlineentitlements.BaseOfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast1OfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest :
    BaseOfflineEntitlementsWithInitialRequestsAndInitialPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast1OfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest :
    BaseOfflineEntitlementsWithInitialRequestsAndNoInitialPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
