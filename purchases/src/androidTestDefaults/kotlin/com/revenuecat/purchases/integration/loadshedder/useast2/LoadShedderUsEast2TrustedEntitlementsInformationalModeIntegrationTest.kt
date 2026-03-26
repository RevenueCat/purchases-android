package com.revenuecat.purchases.integration.loadshedder.useast2

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.integration.production.trustedentitlements.TrustedEntitlementsInformationalModeIntegrationTest

class LoadShedderUsEast2TrustedEntitlementsInformationalModeIntegrationTest :
    TrustedEntitlementsInformationalModeIntegrationTest() {
    override val environmentConfig get() = Constants.loadShedderUsEast2
}
