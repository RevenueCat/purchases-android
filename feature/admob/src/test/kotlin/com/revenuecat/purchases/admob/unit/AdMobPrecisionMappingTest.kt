@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdValue
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import org.junit.Assert.assertEquals
import org.junit.Test

class AdMobPrecisionMappingTest {

    @Test
    fun `precise maps to exact`() {
        assertEquals(
            AdRevenuePrecision.EXACT,
            AdValue.PrecisionType.PRECISE.toAdRevenuePrecision(),
        )
    }

    @Test
    fun `estimated maps to estimated`() {
        assertEquals(
            AdRevenuePrecision.ESTIMATED,
            AdValue.PrecisionType.ESTIMATED.toAdRevenuePrecision(),
        )
    }

    @Test
    fun `publisher provided maps to publisher defined`() {
        assertEquals(
            AdRevenuePrecision.PUBLISHER_DEFINED,
            AdValue.PrecisionType.PUBLISHER_PROVIDED.toAdRevenuePrecision(),
        )
    }

    @Test
    fun `unknown value maps to unknown`() {
        assertEquals(
            AdRevenuePrecision.UNKNOWN,
            Int.MIN_VALUE.toAdRevenuePrecision(),
        )
    }
}
