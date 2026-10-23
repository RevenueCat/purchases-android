@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.iam

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.utils.stubStoreTransactionFromGooglePurchase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Scenarios 7 and 8: user-scoped writes wait for identity changes, and identity changes take turns.
 *
 * A held request also blocks the SDK's single backend worker, so "nothing was sent yet" proves nothing here.
 * What each scenario checks instead is which user the later request was built for: without the sequencing it
 * would be the outgoing user.
 */
internal class IAMSequencingEndToEndTest : IAMEndToEndTest() {

    private val oidc = Identity.oidc("oidc-id-token".toByteArray())

    @Test
    fun `a purchase arriving during the bootstrap posts its receipt as the bootstrapped user`() {
        backend.adoptsAnonymousAppUserID = false
        configure()
        val localID = currentAppUserID
        backend.hold("/auth/login")
        foreground()
        awaitUntil("the bootstrap login in flight") { backend.requests("/auth/login").isNotEmpty() }

        billing.deliverPurchase(stubStoreTransactionFromGooglePurchase(listOf("monthly"), PURCHASE_TIME))
        idleMainLooper()

        backend.release("/auth/login")
        awaitUntil("the receipt post") { backend.requests("/v1/receipts").isNotEmpty() }
        awaitQuiet()

        val receipt = backend.requests("/v1/receipts").single()
        assertThat(currentAppUserID).isNotEqualTo(localID)
        assertThat(customerInfoUpdates.last().originalAppUserId).isEqualTo(currentAppUserID)
        assertThat(customerInfoUpdates.last().activeSubscriptions).containsExactly("monthly")
        assertThat(receipt.user()).isEqualTo(currentAppUserID)
        assertThat(receipt.body?.getString("app_user_id")).isEqualTo(currentAppUserID)
    }

    @Test
    fun `a restore during an explicit login runs after it, as the identified user`() {
        configure()
        foregroundAndBootstrap()
        billing.ownedPurchases.add(stubStoreTransactionFromGooglePurchase(listOf("monthly"), PURCHASE_TIME))
        backend.hold("/auth/login")
        var login: Result<CustomerInfo>? = null
        purchases.authentication.logIn(oidc, recordingCallback { login = it })
        awaitUntil("the explicit login in flight") { backend.requests("/auth/login").size == 2 }

        var restore: Result<CustomerInfo>? = null
        purchases.restorePurchases(recordingCallback { restore = it })
        idleMainLooper()

        backend.release("/auth/login")
        awaitUntil("the login and restore results") { login != null && restore != null }
        awaitQuiet()

        assertThat(login?.getOrThrow()?.originalAppUserId).isEqualTo(IDENTIFIED)
        assertThat(restore?.getOrThrow()?.originalAppUserId).isEqualTo(IDENTIFIED)
        assertThat(restore?.getOrThrow()?.activeSubscriptions).containsExactly("monthly")
        assertThat(backend.requests("/v1/receipts").map { it.user() }).containsOnly(IDENTIFIED)
    }

    @Test
    fun `an explicit login during the bootstrap waits for it and links its identity`() {
        backend.adoptsAnonymousAppUserID = false
        configure()
        backend.hold("/auth/login")
        foreground()
        awaitUntil("the bootstrap login in flight") { backend.requests("/auth/login").isNotEmpty() }

        var login: Result<CustomerInfo>? = null
        purchases.authentication.logIn(oidc, recordingCallback { login = it })
        idleMainLooper()

        backend.release("/auth/login")
        awaitUntil("the explicit login") { login != null }
        assertThat(login?.getOrThrow()?.originalAppUserId).isEqualTo(IDENTIFIED)
        awaitQuiet()

        val (bootstrap, explicit) = backend.requests("/auth/login")
        assertThat(bootstrap.body?.getString("method")).isEqualTo("anonymous")
        assertThat(explicit.body?.getString("method")).isEqualTo("oidc")
        assertThat(explicit.body?.getString("link_to_id")).isEqualTo(backend.issuedTokens.first().getString("id_token"))
        assertThat(currentAppUserID).isEqualTo(IDENTIFIED)
    }

    @Test
    fun `repeated foregrounds during the bootstrap don't start a second one`() {
        configure()
        backend.hold("/auth/login")
        foreground()
        awaitUntil("the bootstrap login in flight") { backend.requests("/auth/login").isNotEmpty() }

        background()
        foreground()
        background()
        foreground()
        backend.release("/auth/login")
        awaitUntil("the bootstrap's token") { delegate.tokens.isNotEmpty() }
        awaitQuiet()

        assertThat(backend.requests("/auth/login")).hasSize(1)
    }

    companion object {
        private const val IDENTIFIED = "identified-user"
        private const val PURCHASE_TIME = 1_792_000_000_000L
    }
}
