package com.revenuecat.purchases

import com.revenuecat.purchases.common.GoogleOfferingParser
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.errorLog

internal object OfferingParserFactory {

    fun createOfferingParser(
        store: Store,
    ) = when (store) {
        Store.PLAY_STORE -> GoogleOfferingParser()
        Store.AMAZON -> {
            try {
                Class.forName("com.revenuecat.purchases.amazon.AmazonOfferingParser")
                    .getConstructor().newInstance() as OfferingParser
            } catch (e: ClassNotFoundException) {
                errorLog(e) { "Make sure purchases-amazon is added as dependency" }
                throw e
            }
        }
        else -> {
            errorLog { "Incompatible store ($store) used" }
            throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
        }
    }
}
