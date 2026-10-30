@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.iam

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Step A: spending virtual currencies, an IAM-only, user-scoped write. */
internal class IAMVirtualCurrencyEndToEndTest : IAMEndToEndTest() {

    private val oidc = Identity.oidc("oidc-id-token".toByteArray())

    @Test
    fun `a spend debits the user's balance with their token and caches the new balances`() {
        configure()
        foregroundAndBootstrap()
        val user = currentAppUserID
        backend.setBalance(user, COIN, START)

        val result = awaitVirtualCurrencies { purchases.spendVirtualCurrency(COIN, 3, "order-1", it) }

        assertThat(result.getOrThrow()[COIN]?.balance).isEqualTo(START - 3)
        assertThat(backend.balance(user, COIN)).isEqualTo(START - 3)
        val spend = backend.requests(SPEND).single()
        assertThat(spend.user()).isEqualTo(user)
        assertThat(spend.body?.getJSONObject("adjustments")?.getInt(COIN)).isEqualTo(3)
        assertThat(spend.body?.getString("reference")).isEqualTo("order-1")
        assertThat(spend.idempotencyKey).isNotBlank()
        assertThat(purchases.cachedVirtualCurrencies?.get(COIN)?.balance).isEqualTo(START - 3)
    }

    @Test
    fun `each spend is its own transaction`() {
        configure()
        foregroundAndBootstrap()
        backend.setBalance(currentAppUserID, COIN, START)

        awaitVirtualCurrencies { purchases.spendVirtualCurrency(COIN, 1, callback = it) }.getOrThrow()
        val second = awaitVirtualCurrencies { purchases.spendVirtualCurrency(COIN, 1, callback = it) }

        assertThat(second.getOrThrow()[COIN]?.balance).isEqualTo(START - 2)
        assertThat(backend.requests(SPEND).map { it.idempotencyKey }.distinct()).hasSize(2)
    }

    @Test
    fun `a spend retried after a token refresh keeps its Idempotency-Key and is applied once`() {
        configure()
        foregroundAndBootstrap()
        val user = currentAppUserID
        backend.setBalance(user, COIN, START)
        backend.expire(delegate.tokens.single()!!)

        val result = awaitVirtualCurrencies { purchases.spendVirtualCurrency(COIN, 3, callback = it) }

        assertThat(result.getOrThrow()[COIN]?.balance).isEqualTo(START - 3)
        assertThat(backend.requests("/auth/token")).hasSize(1)
        val attempts = backend.requests(SPEND)
        assertThat(attempts).hasSize(2)
        assertThat(attempts.map { it.idempotencyKey }.distinct()).hasSize(1)
        assertThat(backend.balance(user, COIN)).isEqualTo(START - 3)
    }

    @Test
    fun `a spend during an explicit login is applied to the identified user`() {
        configure()
        foregroundAndBootstrap()
        val anonymous = currentAppUserID
        backend.setBalance(anonymous, COIN, START)
        backend.setBalance(IDENTIFIED, COIN, START)
        backend.hold("/auth/login")
        var login: Result<CustomerInfo>? = null
        purchases.authentication.logIn(oidc, recordingCallback { login = it })
        awaitUntil("the explicit login in flight") { backend.requests("/auth/login").size == 2 }

        var spend: Result<VirtualCurrencies>? = null
        purchases.spendVirtualCurrency(COIN, 3, callback = recordingVirtualCurrenciesCallback { spend = it })
        idleMainLooper()
        backend.release("/auth/login")
        awaitUntil("the login and spend results") { login != null && spend != null }

        assertThat(login?.getOrThrow()?.originalAppUserId).isEqualTo(IDENTIFIED)
        assertThat(spend?.getOrThrow()?.get(COIN)?.balance).isEqualTo(START - 3)
        assertThat(backend.requests(SPEND).single().user()).isEqualTo(IDENTIFIED)
        assertThat(backend.balance(IDENTIFIED, COIN)).isEqualTo(START - 3)
        assertThat(backend.balance(anonymous, COIN)).isEqualTo(START)
    }

    @Test
    fun `with a multithreaded executor, spends finish in order and the cache holds the newest balance`() {
        val executor = Executors.newFixedThreadPool(THREADS)
        try {
            configure(service = executor)
            foregroundAndBootstrap()
            val user = currentAppUserID
            backend.setBalance(user, COIN, START)
            // The first spend's response (balance 9) arrives late. Run concurrently, the second spend's
            // response (balance 8) would land first, and the late 9 would then overwrite it in the cache.
            backend.delayOnce(SPEND, LATE_MS)
            val results = CopyOnWriteArrayList<Result<VirtualCurrencies>>()

            purchases.spendVirtualCurrency(COIN, 1, callback = recordingVirtualCurrenciesCallback { results.add(it) })
            purchases.spendVirtualCurrency(COIN, 1, callback = recordingVirtualCurrenciesCallback { results.add(it) })
            awaitUntil("both spend results") { results.size == 2 }
            val requestsBefore = backend.requests.size

            val read = awaitVirtualCurrencies { purchases.getVirtualCurrencies(it) }

            assertThat(backend.balance(user, COIN)).isEqualTo(START - 2)
            assertThat(read.getOrThrow()[COIN]?.balance).isEqualTo(START - 2)
            assertThat(backend.requests).hasSize(requestsBefore)
            assertThat(results.map { it.getOrThrow()[COIN]?.balance }).containsExactly(START - 1, START - 2)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `with a multithreaded executor, a slow fetch can't overwrite a later spend's balance`() {
        val executor = Executors.newFixedThreadPool(THREADS)
        try {
            configure(service = executor)
            foregroundAndBootstrap()
            val user = currentAppUserID
            backend.setBalance(user, COIN, START)
            // The fetch's response (balance 10) arrives late. Run concurrently, the spend's response (balance 9)
            // would land first, and the late 10 would then overwrite it in the cache.
            backend.delayOnce(VIRTUAL_CURRENCIES, LATE_MS)
            var fetch: Result<VirtualCurrencies>? = null
            var spend: Result<VirtualCurrencies>? = null

            purchases.getVirtualCurrencies(recordingVirtualCurrenciesCallback { fetch = it })
            purchases.spendVirtualCurrency(COIN, 1, callback = recordingVirtualCurrenciesCallback { spend = it })
            awaitUntil("the fetch and spend results") { fetch != null && spend != null }
            val requestsBefore = backend.requests.size

            val read = awaitVirtualCurrencies { purchases.getVirtualCurrencies(it) }

            assertThat(backend.balance(user, COIN)).isEqualTo(START - 1)
            assertThat(read.getOrThrow()[COIN]?.balance).isEqualTo(START - 1)
            assertThat(backend.requests).hasSize(requestsBefore)
            assertThat(fetch?.getOrThrow()?.get(COIN)?.balance).isEqualTo(START)
            assertThat(spend?.getOrThrow()?.get(COIN)?.balance).isEqualTo(START - 1)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a rejected spend fails and leaves the cached balances alone`() {
        configure()
        foregroundAndBootstrap()
        backend.setBalance(currentAppUserID, COIN, 2)
        awaitVirtualCurrencies { purchases.getVirtualCurrencies(it) }.getOrThrow()

        val result = awaitVirtualCurrencies { purchases.spendVirtualCurrency(COIN, 5, callback = it) }

        assertThat(result.isFailure).isTrue()
        assertThat(backend.balance(currentAppUserID, COIN)).isEqualTo(2)
        assertThat(purchases.cachedVirtualCurrencies?.get(COIN)?.balance).isEqualTo(2)
    }

    @Test
    fun `a spend with no positive amounts sends nothing and returns the current balances`() {
        configure()
        foregroundAndBootstrap()
        backend.setBalance(currentAppUserID, COIN, START)

        val result = awaitVirtualCurrencies { purchases.spendVirtualCurrencies(mapOf(COIN to 0), callback = it) }

        assertThat(result.getOrThrow()[COIN]?.balance).isEqualTo(START)
        assertThat(backend.requests(SPEND)).isEmpty()
        assertThat(backend.requests("/v1/customer/virtual_currencies")).hasSize(1)
    }

    @Test
    fun `with IAM disabled a spend fails as unsupported and makes no request`() {
        configure(iamEnabled = false)

        val result = awaitVirtualCurrencies { purchases.spendVirtualCurrency(COIN, 1, callback = it) }
        awaitQuiet()

        assertThat((result.exceptionOrNull() as? PurchasesErrorException)?.error?.code)
            .isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(backend.requests).isEmpty()
    }

    private fun awaitVirtualCurrencies(
        start: (GetVirtualCurrenciesCallback) -> Unit,
    ): Result<VirtualCurrencies> {
        var result: Result<VirtualCurrencies>? = null
        start(recordingVirtualCurrenciesCallback { result = it })
        awaitUntil("a VirtualCurrencies result") { result != null }
        return result!!
    }

    private fun recordingVirtualCurrenciesCallback(onResult: (Result<VirtualCurrencies>) -> Unit) =
        object : GetVirtualCurrenciesCallback {
            override fun onReceived(virtualCurrencies: VirtualCurrencies) =
                onResult(Result.success(virtualCurrencies))

            override fun onError(error: PurchasesError) = onResult(Result.failure(PurchasesErrorException(error)))
        }

    companion object {
        private const val COIN = "COIN"
        private const val START = 10
        private const val IDENTIFIED = "identified-user"
        private const val SPEND = "/v1/customer/virtual_currencies/spend"
        private const val VIRTUAL_CURRENCIES = "/v1/customer/virtual_currencies"
        private const val THREADS = 4
        private const val LATE_MS = 500L
    }
}
