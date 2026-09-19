package com.revenuecat.purchases.integration.production.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.identity.BaseCustomerInfoIdentityRaceIntegrationTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionCustomerInfoIdentityRaceIntegrationTest : BaseCustomerInfoIdentityRaceIntegrationTest() {
    override val environmentConfig get() = Constants.production
}
