package com.revenuecat.purchases.integration.production.fallbackurl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.fallbackurl.BasePurchasesFallbackURLTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionPurchasesFallbackURLTest : BasePurchasesFallbackURLTest() {
    override val environmentConfig get() = Constants.production
}
