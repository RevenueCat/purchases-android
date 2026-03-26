package com.revenuecat.purchases.integration.loadshedder.useast2

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.fallbackurl.PurchasesFallbackURLTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadShedderUsEast2PurchasesFallbackURLTest : PurchasesFallbackURLTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
