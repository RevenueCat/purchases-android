package com.revenuecat.purchases

import com.revenuecat.purchases.amazon.AmazonOfferingParser
import com.revenuecat.purchases.common.GoogleOfferingParser
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.galaxy.GalaxyOfferingParser
import com.revenuecat.purchases.simulatedstore.SimulatedStoreOfferingParser

internal object OfferingParserFactory {

    fun createOfferingParser(
        store: Store,
        shouldParsePaywallComponents: () -> Boolean = { true },
    ): OfferingParser {
        return when (store) {
            Store.TEST_STORE -> SimulatedStoreOfferingParser(shouldParsePaywallComponents)
            Store.PLAY_STORE -> GoogleOfferingParser(shouldParsePaywallComponents)
            Store.AMAZON -> AmazonOfferingParser(shouldParsePaywallComponents)
            Store.GALAXY -> GalaxyOfferingParser(shouldParsePaywallComponents)
            else -> {
                errorLog { "Incompatible store ($store) used" }
                throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
            }
        }
    }
}
