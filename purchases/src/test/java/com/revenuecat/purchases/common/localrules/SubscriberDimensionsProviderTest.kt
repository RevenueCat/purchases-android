@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensions
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsResolution
import com.revenuecat.purchases.rules.RulesEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberDimensionsProviderTest {

    private val evaluationDate = Date(1_718_452_800_000)
    private val discarded = mutableListOf<SubscriberDimensions>()

    @Test
    fun `the config copy is used when it is the only one`() = runTest {
        val dimensions = provider(config = dimensions(asOf = 100, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
        assertThat(discarded).isEmpty()
    }

    @Test
    fun `the purchase copy is used when the topic is not configured`() = runTest {
        val dimensions = provider(receipt = dimensions(asOf = 100, "plan" to "annual")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
        assertThat(discarded).isEmpty()
    }

    @Test
    fun `a newer purchase copy wins and is kept`() = runTest {
        val dimensions = provider(
            config = dimensions(asOf = 100, "plan" to "annual"),
            receipt = dimensions(asOf = 200, "plan" to "monthly"),
        ).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("monthly")))
        assertThat(discarded).isEmpty()
    }

    @Test
    fun `a newer config copy wins and discards the purchase copy`() = runTest {
        val receipt = dimensions(asOf = 100, "plan" to "monthly")

        val dimensions = provider(
            config = dimensions(asOf = 200, "plan" to "annual"),
            receipt = receipt,
        ).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("annual")))
        assertThat(discarded).containsExactly(receipt)
    }

    @Test
    fun `the purchase copy wins a tie and is kept`() = runTest {
        val dimensions = provider(
            config = dimensions(asOf = 100, "plan" to "annual"),
            receipt = dimensions(asOf = 100, "plan" to "monthly"),
        ).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(mapOf("plan" to RulesDimensionValue.StringValue("monthly")))
        assertThat(discarded).isEmpty()
    }

    @Test
    fun `the copies are never merged`() = runTest {
        val dimensions = provider(
            config = dimensions(asOf = 200, "plan" to "annual"),
            receipt = dimensions(asOf = 100, "seats" to "3"),
        ).dimensions(evaluationDate)

        assertThat(dimensions).containsOnlyKeys("plan")
    }

    @Test
    fun `nothing is contributed when neither source has dimensions`() = runTest {
        assertThat(provider().dimensions(evaluationDate)).isEmpty()
    }

    @Test
    fun `a purchase copy that cannot be read leaves the config copy and the other dimensions usable`() = runTest {
        val snapshot = resolver(
            deviceProvider("platform" to "android"),
            SubscriberDimensionsProvider(
                configDimensions = { SubscriberDimensionsResolution.Found(dimensions(asOf = 100, "plan" to "annual")) },
                receiptDimensions = { throw IllegalStateException("no cache") },
                discardReceiptDimensions = { discarded.add(it) },
            ),
        ).snapshot()

        assertThat(snapshot.isSuccess).isTrue()
        assertThat(snapshot.getOrThrow().values).containsOnlyKeys("evaluated_at", "platform", "plan")
        assertThat(discarded).isEmpty()
    }

    @Test
    fun `a dimension colliding with an SDK-provided one fails the snapshot`() = runTest {
        // Same treatment as any other source: the root names are one contract, and the backend's side of it is
        // to not claim a name the SDK already exposes.
        val error = resolver(
            deviceProvider("platform" to "android"),
            provider(receipt = dimensions(asOf = 100, "platform" to "spoofed", "plan" to "annual")),
        ).snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("platform"))
    }

    @Test
    fun `the dimensions are readable by a predicate`() = runTest {
        val receipt = SubscriberDimensions(
            values = mapOf(
                "plan" to RulesDimensionValue.StringValue("annual"),
                "seats" to RulesDimensionValue.IntValue(3),
                "profile" to RulesDimensionValue.ObjectValue(mapOf("tier" to RulesDimensionValue.StringValue("gold"))),
                "gone" to RulesDimensionValue.NullValue,
            ),
            asOf = Date(100),
        )
        val values = resolver(provider(receipt = receipt)).snapshot().getOrThrow().values

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

    private fun dimensions(asOf: Long, vararg values: Pair<String, String>) = SubscriberDimensions(
        values = values.associate { (key, value) -> key to RulesDimensionValue.StringValue(value) },
        asOf = Date(asOf),
    )

    private fun provider(
        config: SubscriberDimensions? = null,
        receipt: SubscriberDimensions? = null,
    ) = SubscriberDimensionsProvider(
        configDimensions = {
            config?.let { SubscriberDimensionsResolution.Found(it) } ?: SubscriberDimensionsResolution.NotConfigured
        },
        receiptDimensions = { receipt },
        discardReceiptDimensions = { discarded.add(it) },
    )

    private fun resolver(vararg providers: RulesDimensionProvider) = RulesDimensionResolver(
        providers = providers.toList(),
        currentAppUserId = { "user" },
        dateProvider = object : DateProvider {
            override val now: Date = evaluationDate
        },
    )

    private fun deviceProvider(vararg values: Pair<String, String>) = object : RulesDimensionProvider {
        override val name = "device"
        override suspend fun dimensions(date: Date) =
            values.associate { (key, value) -> key to RulesDimensionValue.StringValue(value) }
    }
}
