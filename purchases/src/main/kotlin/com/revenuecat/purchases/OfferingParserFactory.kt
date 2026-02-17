package com.revenuecat.purchases

import com.revenuecat.purchases.amazon.AmazonOfferingParser
import com.revenuecat.purchases.common.GoogleOfferingParser
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.simulatedstore.SimulatedStoreOfferingParser

internal object OfferingParserFactory {

    fun createOfferingParser(
        store: Store,
    ): OfferingParser {
        return when (store) {
            Store.TEST_STORE -> SimulatedStoreOfferingParser()
            Store.PLAY_STORE -> GoogleOfferingParser()
            Store.AMAZON -> AmazonOfferingParser()
            else -> {
                errorLog { "Incompatible store ($store) used" }
                throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
            }
        }
    }
}
