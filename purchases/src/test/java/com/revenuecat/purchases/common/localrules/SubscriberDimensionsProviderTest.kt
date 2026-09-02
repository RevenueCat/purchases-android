@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
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

    @Test
    fun `every value shape a rule can read is kept`() = runTest {
        val dimensions = provider(
            """
            {
                "plan": "annual",
                "beta": true,
                "seats": 3,
                "score": 0.75,
                "profile": {"tier": "gold", "age": 42},
                "teams": [{"id": "a"}, {"id": "b"}]
            }
            """.trimIndent(),
        ).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(
            mapOf(
                "plan" to RulesDimensionValue.StringValue("annual"),
                "beta" to RulesDimensionValue.BoolValue(true),
                "seats" to RulesDimensionValue.IntValue(3),
                "score" to RulesDimensionValue.DoubleValue(0.75),
                "profile" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "tier" to RulesDimensionValue.StringValue("gold"),
                        "age" to RulesDimensionValue.IntValue(42),
                    ),
                ),
                "teams" to RulesDimensionValue.ObjectListValue(
                    listOf(
                        mapOf("id" to RulesDimensionValue.StringValue("a")),
                        mapOf("id" to RulesDimensionValue.StringValue("b")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `an explicit null is kept as null, at the root and inside an object`() = runTest {
        // The backend stated the name and chose null for it, which a rule can compare against.
        val dimensions = provider("""{"gone": null, "profile": {"tier": null, "age": 42}}""")
            .dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(
            mapOf(
                "gone" to RulesDimensionValue.NullValue,
                "profile" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "tier" to RulesDimensionValue.NullValue,
                        "age" to RulesDimensionValue.IntValue(42),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `a value no rule could read is dropped without dropping the others`() = runTest {
        // An object list is the only collection a dimension can be — same treatment the backend predicate
        // results get.
        val dimensions = provider("""{"codes": [1, 2], "mixed": [{"id": "a"}, null], "plan": "annual"}""")
            .dimensions(evaluationDate)

        assertThat(dimensions).containsOnlyKeys("plan")
    }

    @Test
    fun `a cache that is not a JSON object contributes nothing`() = runTest {
        val unreadable = listOf("not json at all", """["an", "array"]""", "\"a string\"", "42")

        for (json in unreadable) {
            assertThat(provider(json).dimensions(evaluationDate)).describedAs(json).isEmpty()
        }
    }

    @Test
    fun `a customer with no cached dimensions contributes nothing`() = runTest {
        assertThat(provider(null).dimensions(evaluationDate)).isEmpty()
    }

    @Test
    fun `a cache that cannot be read leaves the other dimensions usable`() = runTest {
        val snapshot = resolver(
            deviceProvider("platform" to "android"),
            SubscriberDimensionsProvider { throw IllegalStateException("no cache") },
        ).snapshot()

        assertThat(snapshot.isSuccess).isTrue()
        assertThat(snapshot.getOrThrow().values).containsOnlyKeys("evaluated_at", "platform")
    }

    @Test
    fun `the cache is read on every evaluation`() = runTest {
        var json: String? = """{"plan": "annual"}"""
        val provider = SubscriberDimensionsProvider { json }

        assertThat(provider.dimensions(evaluationDate))
            .containsEntry("plan", RulesDimensionValue.StringValue("annual"))

        json = """{"plan": "monthly"}"""

        assertThat(provider.dimensions(evaluationDate))
            .containsEntry("plan", RulesDimensionValue.StringValue("monthly"))
    }

    @Test
    fun `a dimension colliding with an SDK-provided one fails the snapshot`() = runTest {
        // Same treatment as any other source: the root names are one contract, and the backend's side of it is
        // to not claim a name the SDK already exposes.
        val error = resolver(
            deviceProvider("platform" to "android"),
            provider("""{"platform": "spoofed", "plan": "annual"}"""),
        ).snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("platform"))
    }

    @Test
    fun `the dimensions are readable by a predicate`() = runTest {
        val values = resolver(
            provider("""{"plan": "annual", "seats": 3, "profile": {"tier": "gold"}, "gone": null}"""),
        ).snapshot().getOrThrow().values

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

    private fun provider(json: String?) = SubscriberDimensionsProvider { json }

    private fun resolver(vararg providers: RulesDimensionProvider) = RulesDimensionResolver(
        providers = providers.toList(),
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
