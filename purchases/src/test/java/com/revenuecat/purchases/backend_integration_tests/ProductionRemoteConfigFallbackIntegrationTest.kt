package com.revenuecat.purchases.backend_integration_tests

import android.content.Context
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobFetcher
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDiskCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Test
import java.io.File

/**
 * Exercises the remote-config fallback endpoint against the real fallback host
 * (`https://api-production.8-lives-cat.io/`). Unlike the main endpoint this is a plain `GET` returning an
 * `application/json` [RemoteConfiguration] body (no `x-rc-format`, no inlined blobs, no request body, no nonce).
 *
 * [forceServerErrorStrategy] is [ForceServerErrorStrategy.failExceptFallbackUrls]: the direct-endpoint tests
 * call the fallback host (which is never faked) so they hit the real endpoint, and the manager-flow test relies
 * on the main `api.revenuecat.com` request being forced to a `5xx` so the domain layer routes to the fallback.
 */
internal class ProductionRemoteConfigFallbackIntegrationTest : BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey
    override val forceServerErrorStrategy: ForceServerErrorStrategy = ForceServerErrorStrategy.failExceptFallbackUrls

    private val testFolder = "temp_production_remote_config_fallback_integration_test_folder"

    private lateinit var remoteConfigBlobStore: RemoteConfigBlobStore

    @After
    fun tearDownRemoteConfigStorage() {
        File(testFolder).deleteRecursively()
    }

    @Test
    fun `can fetch remote config from the fallback endpoint`() {
        val (error, config) = fetchRemoteConfigFallback()

        assertThat(error).isNull()
        val remoteConfiguration = requireNotNull(config) { "Expected a fallback config response." }
        assertThat(remoteConfiguration.domain).isEqualTo("app")
        assertThat(remoteConfiguration.manifest).isNotEmpty()
        assertThat(remoteConfiguration.activeTopics).contains("sources", "ui_config", "workflows")

        verify(exactly = 1) {
            // The fallback endpoint is a plain-JSON GET, so its response is ETag-cached under the fallback URL,
            // confirming the exact host + path we hit.
            sharedPreferencesEditor.putString(
                "https://api-production.8-lives-cat.io/v1/config/app",
                any(),
            )
        }
        // The fallback response is not signed with a nonce; with verification disabled no verification runs.
        assertSigningNotPerformed()
    }

    @Test
    fun `verifies the fallback response without a nonce when verification is enforced`() {
        setupTest(SignatureVerificationMode.Enforced())

        val (error, config) = fetchRemoteConfigFallback()

        // Under enforcement a failed verification surfaces as an error, so a null error already implies the
        // response verified (statically, without a nonce). Assert the signing call happened explicitly too.
        assertThat(error).isNull()
        assertThat(config).isNotNull
        assertSigningPerformed()
    }

    @Test
    fun `a second fallback request is served from the ETag cache`() {
        every { appConfig.isDebugBuild } returns false
        // The base prefs mock does not persist, so back it with a real map: otherwise the stored ETag is never
        // read back and the second request could not send `X-RevenueCat-ETag`.
        val prefsBacking = mutableMapOf<String, String?>()
        every { sharedPreferences.getString(any(), any()) } answers { prefsBacking[firstArg()] ?: secondArg() as String? }
        every { sharedPreferencesEditor.putString(any(), any()) } answers {
            prefsBacking[firstArg()] = secondArg()
            sharedPreferencesEditor
        }

        val (firstError, firstConfig) = fetchRemoteConfigFallback()
        val (secondError, secondConfig) = fetchRemoteConfigFallback()

        assertThat(firstError).isNull()
        assertThat(secondError).isNull()
        // The cached response is returned verbatim, so the second config equals the first.
        assertThat(secondConfig).isEqualTo(firstConfig)

        // The first `200` is stored under the fallback URL; the second request sends the stored ETag, the server
        // replies `304 Not Modified`, and the SDK serves the cached result. A `304` is not re-stored, so exactly
        // one write to the fallback URL key proves the ETag round-trip happened (a plain re-fetch would store twice).
        verify(exactly = 1) {
            sharedPreferencesEditor.putString(
                "https://api-production.8-lives-cat.io/v1/config/app",
                any(),
            )
        }
    }

    @Test
    fun `domain layer falls back to the fallback endpoint when the main request fails with no cached config`() {
        every { appConfig.isDebugBuild } returns false
        val manager = buildRemoteConfigManager()

        // A cold read with an empty cache triggers an on-demand sync: the main /v1/config request is forced to a
        // 5xx (failExceptFallbackUrls), so the domain layer routes to the fallback endpoint, whose plain-JSON
        // config it parses and commits — making the topic readable.
        val sources = runBlocking { manager.topic(RemoteConfigTopic.Sources) }

        assertThat(sources).withFailMessage { "Expected the sources topic to be committed from the fallback." }
            .isNotNull
        assertThat(sources).isNotEmpty
    }

    /**
     * Performs a single fallback request and blocks until it completes, returning the error (or `null`) and the
     * parsed [RemoteConfiguration] (or `null` on error).
     */
    private fun fetchRemoteConfigFallback(): Pair<PurchasesError?, RemoteConfiguration?> {
        every { appConfig.isDebugBuild } returns false

        var error: PurchasesError? = null
        var config: RemoteConfiguration? = null
        ensureBlockFinishes { latch ->
            backend.getRemoteConfigFallback(
                appInBackground = false,
                domain = "app",
                onSuccess = {
                    config = it
                    latch.countDown()
                },
                onError = { purchasesError, _ ->
                    error = purchasesError
                    latch.countDown()
                },
            )
        }
        return error to config
    }

    /**
     * Builds a [RemoteConfigManager] wired to the real [backend] and backed by the real [RemoteConfigDiskCache] +
     * [RemoteConfigBlobStore] on a fresh temp folder (empty cache → cold start). The blob fetcher's network layer
     * is mocked out (the fallback response carries no inlined blobs, and topic reads need no blob), so only the
     * fetch -> persist -> disk -> read path is exercised.
     */
    private fun buildRemoteConfigManager(): RemoteConfigManager {
        val context = mockk<Context>()
        every { context.noBackupFilesDir } returns File(testFolder).apply { mkdirs() }
        remoteConfigBlobStore = RemoteConfigBlobStore(context)
        val blobFetcher = mockk<RemoteConfigBlobFetcher>(relaxed = true)
        coEvery { blobFetcher.ensureDownloaded(any<String>()) } answers { remoteConfigBlobStore.contains(firstArg()) }
        return RemoteConfigManager(
            backend = backend,
            diskCache = RemoteConfigDiskCache(context),
            blobStore = remoteConfigBlobStore,
            blobFetcher = blobFetcher,
            appUserIDProvider = { REMOTE_CONFIG_USER },
        )
    }

    private companion object {
        private const val REMOTE_CONFIG_USER = "integrationTestRemoteConfigFallbackUser"
    }
}
