@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.identity

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.Test

class AuthenticationDelegateTest {

    @Test
    fun `a delegate implementing only the error method ignores access-token updates`() {
        val delegate = object : AuthenticationDelegate {
            override fun authenticatorDidEncounterError(error: PurchasesError) = Unit
        }

        assertThatCode {
            delegate.authenticatorDidUpdateAccessToken("access")
            delegate.authenticatorDidUpdateAccessToken(null)
        }.doesNotThrowAnyException()
    }
}
