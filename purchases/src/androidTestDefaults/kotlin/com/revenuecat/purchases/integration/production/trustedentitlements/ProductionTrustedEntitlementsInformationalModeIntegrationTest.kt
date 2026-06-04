package com.revenuecat.purchases.integration.production.trustedentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.trustedentitlements.BaseTrustedEntitlementsInformationalModeIntegrationTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionTrustedEntitlementsInformationalModeIntegrationTest :
    BaseTrustedEntitlementsInformationalModeIntegrationTest() {
    override val environmentConfig get() = Constants.production
}
