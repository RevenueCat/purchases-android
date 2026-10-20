package com.revenuecat.purchases.iam

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.utils.stubStoreTransactionFromGooglePurchase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Scenarios 1–3 and 9: configure, the silent anonymous login, cold starts and token storage. */
internal class IAMBootstrapEndToEndTest : IAMEndToEndTest() {

    @Test
    fun `configuring without a foreground makes no requests`() {
        configure()

        awaitQuiet()

        assertThat(backend.requests).isEmpty()
    }

    @Test
    fun `a pending purchase at configure isn't posted before the bootstrap`() {
        configure()
        billing.ownedPurchases.add(stubStoreTransactionFromGooglePurchase(listOf("monthly"), PURCHASE_TIME))
        billing.startConnection()

        awaitQuiet()

        assertThat(backend.requests).isEmpty()
    }

    @Test
    fun `the first foreground logs in anonymously once, migrating the local anonymous ID`() {
        configure()
        val localID = currentAppUserID

        foregroundAndBootstrap()

        val login = backend.requests("/auth/login").single()
        assertThat(login.body?.getString("method")).isEqualTo("anonymous")
        assertThat(login.body?.getString("app_user_id")).isEqualTo(localID)
        assertThat(login.authorization).isEqualTo("Bearer $API_KEY")
        assertThat(currentAppUserID).isEqualTo(localID)
        assertThat(delegate.tokens.map { backend.ownerOf(it) }).containsExactly(localID)
    }

    @Test
    fun `after the bootstrap, every request uses the user's token on IAM paths`() {
        configure()

        foregroundAndBootstrap()

        val afterLogin = backend.requests.dropWhile { it.path != "/auth/login" }.drop(1)
        assertThat(afterLogin.map { it.path }).contains("/v1/customer", "/v1/customer/offerings")
        assertThat(afterLogin.map { it.path }).noneMatch { it.startsWith("/v1/subscribers") }
        assertThat(afterLogin.map { it.user() }).containsOnly(currentAppUserID)
        assertThat(customerInfoUpdates.map { it.originalAppUserId }).containsExactly(currentAppUserID)
    }

    @Test
    fun `a server-assigned ID replaces the local one and becomes the user`() {
        backend.adoptsAnonymousAppUserID = false
        configure()
        val localID = currentAppUserID

        foregroundAndBootstrap()

        assertThat(currentAppUserID).isNotEqualTo(localID).startsWith("server-anonymous-")
        assertThat(purchases.isAnonymous).isTrue()
        assertThat(customerInfoUpdates.map { it.originalAppUserId }).containsExactly(currentAppUserID)
        val afterLogin = backend.requests.dropWhile { it.path != "/auth/login" }.drop(1)
        assertThat(afterLogin.map { it.user() }).containsOnly(currentAppUserID)
    }

    @Test
    fun `a relaunch loads the stored tokens and doesn't log in again`() {
        configure()
        foregroundAndBootstrap()
        val user = currentAppUserID
        val token = delegate.tokens.single()
        val before = backend.requests.size

        relaunch()
        foreground()
        awaitUntil("a request after the relaunch") { backend.requests.size > before }
        awaitQuiet()

        val afterRelaunch = backend.requests.drop(before)
        assertThat(afterRelaunch.map { it.path }).doesNotContain("/auth/login")
        assertThat(afterRelaunch.map { it.bearerToken }).containsOnly(token)
        assertThat(currentAppUserID).isEqualTo(user)
        assertThat(purchases.authentication.currentAccessToken).isEqualTo(token)
    }

    @Test
    fun `a pre-IAM anonymous user keeps their ID when IAM is turned on`() {
        configure(iamEnabled = false)
        foreground()
        awaitUntil("the pre-IAM CustomerInfo") { customerInfoUpdates.isNotEmpty() }
        awaitQuiet()
        val legacyID = currentAppUserID

        relaunch(iamEnabled = true)
        foregroundAndBootstrap()

        assertThat(backend.requests("/auth/login").single().body?.getString("app_user_id")).isEqualTo(legacyID)
        assertThat(currentAppUserID).isEqualTo(legacyID)
    }

    @Test
    fun `tokens are stored encrypted`() {
        configure()
        foregroundAndBootstrap()
        val issued = backend.issuedTokens.single()
        val secrets = listOf("access_token", "refresh_token", "id_token").map { issued.getString(it) }

        val dataDir = ApplicationProvider.getApplicationContext<Application>().dataDir
        val plaintextHits = dataDir.walkTopDown().filter { it.isFile }.filter { file ->
            val text = file.readBytes().toString(Charsets.ISO_8859_1)
            secrets.any { it in text }
        }.toList()

        assertThat(plaintextHits).isEmpty()
    }

    @Test
    fun `tokens stored under one API key can't be read with another, so the user bootstraps again`() {
        configure()
        foregroundAndBootstrap()

        relaunch(apiKey = OTHER_API_KEY)
        foregroundAndBootstrap()

        assertThat(backend.requests("/auth/login")).hasSize(2)
        assertThat(backend.requests("/auth/login").last().authorization).isEqualTo("Bearer $OTHER_API_KEY")
    }

    companion object {
        private const val PURCHASE_TIME = 1_792_000_000_000L
        private const val OTHER_API_KEY = "goog_iamEndToEndOtherKey"
    }
}
