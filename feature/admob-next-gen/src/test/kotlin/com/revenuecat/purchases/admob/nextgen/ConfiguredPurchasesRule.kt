@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdTracker
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ConfiguredPurchasesRule : TestRule {
    val adTracker = mockk<AdTracker>(relaxed = true)
    val purchases = mockk<Purchases>(relaxed = true) {
        every { adTracker } returns this@ConfiguredPurchasesRule.adTracker
    }

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            mockkObject(Purchases)
            try {
                every { Purchases.isConfigured } returns true
                every { Purchases.sharedInstance } returns purchases
                base.evaluate()
            } finally {
                unmockkObject(Purchases)
            }
        }
    }
}
