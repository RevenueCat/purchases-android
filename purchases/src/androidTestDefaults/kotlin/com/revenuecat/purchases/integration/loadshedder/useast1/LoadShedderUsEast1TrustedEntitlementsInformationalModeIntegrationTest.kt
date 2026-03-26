package com.revenuecat.purchases.integration.loadshedder.useast1

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.trustedentitlements.TrustedEntitlementsInformationalModeIntegrationTest

class LoadShedderUsEast1TrustedEntitlementsInformationalModeIntegrationTest :
    TrustedEntitlementsInformationalModeIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast1
}
