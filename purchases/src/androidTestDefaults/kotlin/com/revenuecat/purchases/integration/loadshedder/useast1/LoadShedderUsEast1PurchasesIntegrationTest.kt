package com.revenuecat.purchases.integration.loadshedder.useast1

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.BaseIntegrationPurchasesTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast1PurchasesIntegrationTest : BaseIntegrationPurchasesTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
