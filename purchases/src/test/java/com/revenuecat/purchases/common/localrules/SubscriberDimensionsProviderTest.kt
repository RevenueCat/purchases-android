@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensions
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsReceiptStore
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsResolution
import com.revenuecat.purchases.rules.RulesEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberDimensionsProviderTest {

    private val evaluationDate = Date(1_718_452_800_000)
    private val storedJsonByUser = mutableMapOf<String, String>()
    private val deviceCache = mockk<DeviceCache> {
        every { getCachedSubscriberDimensionsJson(any()) } answers { storedJsonByUser[firstArg()] }
        every { cacheSubscriberDimensions(any(), any()) } answers { storedJsonByUser[firstArg()] = secondArg() }
        every { clearSubscriberDimensions(any()) } answers { storedJsonByUser.remove(firstArg<String>()) }
    }
    private val receiptStore = SubscriberDimensionsReceiptStore(deviceCache)

    @Test
    fun `the config copy is used when it is the only one`() = runTest {
        val dimensions = provider(config = dimensions(asOf = 100, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
    }

    @Test
    fun `the purchase copy is used when the topic is not configured`() = runTest {
        storeReceipt(asOf = 100, "plan" to "annual")

        val dimensions = provider().dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
        assertThat(receiptStore.get(USER)).isNotNull()
    }

    @Test
    fun `the purchase copy is used when the config is unavailable`() = runTest {
        storeReceipt(asOf = 100, "plan" to "annual")

        val dimensions = provider(configResolution = SubscriberDimensionsResolution.Unavailable)
            .dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
        assertThat(receiptStore.get(USER)).isNotNull()
    }

    @Test
    fun `a newer purchase copy wins and is kept`() = runTest {
        storeReceipt(asOf = 200, "plan" to "monthly")

        val dimensions = provider(config = dimensions(asOf = 100, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("monthly")))
        assertThat(receiptStore.get(USER)?.asOf).isEqualTo(Date(200))
    }

    @Test
    fun `a newer config copy wins and discards the purchase copy`() = runTest {
        storeReceipt(asOf = 100, "plan" to "monthly")

        val dimensions = provider(config = dimensions(asOf = 200, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
        assertThat(receiptStore.get(USER)).isNull()
    }

    @Test
    fun `the purchase copy wins a tie and is kept`() = runTest {
        storeReceipt(asOf = 100, "plan" to "monthly")

        val dimensions = provider(config = dimensions(asOf = 100, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("monthly")))
        assertThat(receiptStore.get(USER)?.asOf).isEqualTo(Date(100))
    }

    @Test
    fun `the copies are never merged`() = runTest {
        storeReceipt(asOf = 100, "seats" to "3")

        val dimensions = provider(config = dimensions(asOf = 200, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).containsOnlyKeys("plan")
    }

    @Test
    fun `nothing is contributed when neither source has dimensions`() = runTest {
        assertThat(provider().dimensions(evaluationDate)).isEmpty()
    }

    @Test
    fun `the app user is read once so a change during the config read discards that user's copy only`() = runTest {
        storeReceipt(asOf = 100, "plan" to "monthly", user = USER)
        storeReceipt(asOf = 100, "plan" to "monthly", user = OTHER_USER)
        var currentUser = USER
        val provider = SubscriberDimensionsProvider(
            configDimensions = {
                currentUser = OTHER_USER
                SubscriberDimensionsResolution.Found(dimensions(asOf = 200, "plan" to "annual"))
            },
            receiptStore = receiptStore,
            currentAppUserId = { currentUser },
        )

        provider.dimensions(evaluationDate)

        assertThat(receiptStore.get(USER)).isNull()
        assertThat(receiptStore.get(OTHER_USER)).isNotNull()
    }

    @Test
    fun `a purchase copy stored during the config read is used`() = runTest {
        val provider = SubscriberDimensionsProvider(
            configDimensions = {
                storeReceipt(asOf = 300, "plan" to "monthly")
                SubscriberDimensionsResolution.Found(dimensions(asOf = 200, "plan" to "annual"))
            },
            receiptStore = receiptStore,
            currentAppUserId = { USER },
        )

        val dimensions = provider.dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("monthly")))
        assertThat(receiptStore.get(USER)?.asOf).isEqualTo(Date(300))
    }

    @Test
    fun `a purchase copy that cannot be read leaves the config copy and the other dimensions usable`() = runTest {
        val failingCache = mockk<DeviceCache> {
            every { getCachedSubscriberDimensionsJson(any()) } throws IllegalStateException("no cache")
        }
        val snapshot = resolver(
            deviceProvider("platform" to "android"),
            SubscriberDimensionsProvider(
                configDimensions = { SubscriberDimensionsResolution.Found(dimensions(asOf = 100, "plan" to "annual")) },
                receiptStore = SubscriberDimensionsReceiptStore(failingCache),
                currentAppUserId = { USER },
            ),
        ).snapshot()

        assertThat(snapshot.isSuccess).isTrue()
        assertThat(snapshot.getOrThrow().values).containsOnlyKeys("evaluated_at", "platform", "plan")
    }

    @Test
    fun `a dimension colliding with an SDK-provided one fails the snapshot`() = runTest {
        // Same treatment as any other source: the root names are one contract, and the backend's side of it is
        // to not claim a name the SDK already exposes.
        storeReceipt(asOf = 100, "platform" to "spoofed", "plan" to "annual")

        val error = resolver(deviceProvider("platform" to "android"), provider()).snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("platform"))
    }

    @Test
    fun `the dimensions are readable by a predicate`() = runTest {
        receiptStore.store(
            USER,
            JSONObject()
                .put(
                    "dimensions",
                    JSONObject()
                        .put("plan", "annual")
                        .put("seats", 3)
                        .put("profile", JSONObject().put("tier", "gold"))
                        .put("gone", JSONObject.NULL),
                )
                .put("as_of", 100),
        )
        val values = resolver(provider()).snapshot().getOrThrow().values

        val matching = listOf(
            """{"==": [{"var": "plan"}, "annual"]}""",
            """{">": [{"var": "seats"}, 2]}""",
            """{"==": [{"var": "profile.tier"}, "gold"]}""",
            // A null the backend stated is present: `var` resolves it rather than failing or using the default.
            """{"==": [{"var": "gone"}, null]}""",
            """{"==": [{"var": ["gone", "fallback"]}, null]}""",
        )
        val notMatching = listOf(
            """{"==": [{"var": "plan"}, "monthly"]}""",
            """{"!!": {"var": ["never_sent", false]}}""",
            """{"!!": {"var": "gone"}}""",
        )

        for (predicate in matching) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isTrue()
        }
        for (predicate in notMatching) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isFalse()
        }
    }

    private fun storeReceipt(asOf: Long, vararg values: Pair<String, String>, user: String = USER) {
        val dimensions = JSONObject().also { json -> values.forEach { (key, value) -> json.put(key, value) } }
        receiptStore.store(user, JSONObject().put("dimensions", dimensions).put("as_of", asOf))
    }

    private fun dimensions(asOf: Long, vararg values: Pair<String, String>) = SubscriberDimensions(
        values = values.associate { (key, value) -> key to RulesDimensionValue.StringValue(value) },
        asOf = Date(asOf),
    )

    private fun provider(
        config: SubscriberDimensions? = null,
        configResolution: SubscriberDimensionsResolution =
            config?.let { SubscriberDimensionsResolution.Found(it) } ?: SubscriberDimensionsResolution.NotConfigured,
    ) = SubscriberDimensionsProvider(
        configDimensions = { configResolution },
        receiptStore = receiptStore,
        currentAppUserId = { USER },
    )

    private fun resolver(vararg providers: RulesDimensionProvider) = RulesDimensionResolver(
        providers = providers.toList(),
        currentAppUserId = { USER },
        dateProvider = object : DateProvider {
            override val now: Date = evaluationDate
        },
    )

    private fun deviceProvider(vararg values: Pair<String, String>) = object : RulesDimensionProvider {
        override val name = "device"
        override suspend fun dimensions(date: Date) =
            values.associate { (key, value) -> key to RulesDimensionValue.StringValue(value) }
    }

    private companion object {
        const val USER = "user"
        const val OTHER_USER = "other-user"
    }
}
