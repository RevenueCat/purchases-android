
package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import org.junit.Assert.assertEquals
import org.junit.Test

class AdMobPrecisionMappingTest {

    @Test
    fun `maps every precision type`() {
        val expected = mapOf(
            PrecisionType.PRECISE to AdRevenuePrecision.EXACT,
            PrecisionType.ESTIMATED to AdRevenuePrecision.ESTIMATED,
            PrecisionType.PUBLISHER_PROVIDED to AdRevenuePrecision.PUBLISHER_DEFINED,
            PrecisionType.UNKNOWN to AdRevenuePrecision.UNKNOWN,
        )

        // toAdRevenuePrecision falls back to UNKNOWN instead of throwing on unrecognized constants,
        // so this assertion is the only signal that a bump to the pinned SDK grew a precision type.
        assertEquals(
            "Google Mobile Ads added or removed a PrecisionType. Update toAdRevenuePrecision.",
            PrecisionType.values().toSet(),
            expected.keys,
        )
        expected.forEach { (precisionType, revenuePrecision) ->
            assertEquals(revenuePrecision, precisionType.toAdRevenuePrecision())
        }
    }
}
