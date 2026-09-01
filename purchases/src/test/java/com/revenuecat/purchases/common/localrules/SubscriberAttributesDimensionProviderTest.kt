@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider.Companion.KEY_EVALUATED_AT
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider.Companion.KEY_UPDATED_AT
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider.Companion.KEY_VALUE
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberAttributesDimensionProviderTest {

    private val evaluationDate = Date(1_718_452_800_000)

    // Six weeks before the evaluation.
    private val setDate = Date(1_714_780_800_000)
    private val recentSetDate = Date(evaluationDate.time - 1 * 24 * 60 * 60 * 1000L)

    @Test
    fun `every stored attribute is exposed under the name the app set it with`() = runTest {
        val dimensions = provider(
            attribute("\$email", "jane@example.com"),
            attribute("goal", "lose_weight"),
        ).dimensions(evaluationDate)

        // Reserved names keep their '$': it is what the app set, what the dashboard shows, and what keeps a custom
        // "email" from colliding with the reserved one. Only '.' means anything to the engine.
        assertThat(dimensions).containsOnlyKeys("\$email", "goal")
    }

    @Test
    fun `an attribute is a record of its value, when it was set, and when it was read`() = runTest {
        val dimensions = provider(attribute("goal", "lose_weight")).dimensions(evaluationDate)

        assertThat(dimensions).isEqualTo(
            mapOf(
                "goal" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        KEY_VALUE to RulesDimensionValue.StringValue("lose_weight"),
                        KEY_UPDATED_AT to RulesDimensionValue.DateValue(setDate),
                        KEY_EVALUATED_AT to RulesDimensionValue.DateValue(evaluationDate),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `a value stays the string the app set, whatever it looks like`() = runTest {
        val dimensions = provider(
            attribute("seats", "3"),
            attribute("betaOptIn", "true"),
            attribute("sku", "0123"),
        ).dimensions(evaluationDate)

        assertThat(dimensions.valueOf("seats")).isEqualTo(RulesDimensionValue.StringValue("3"))
        assertThat(dimensions.valueOf("betaOptIn")).isEqualTo(RulesDimensionValue.StringValue("true"))
        // A product code the SDK guessed at would silently stop being the string it was set as.
        assertThat(dimensions.valueOf("sku")).isEqualTo(RulesDimensionValue.StringValue("0123"))
    }

    @Test
    fun `a deleted attribute is left out whether or not it has been posted yet`() = runTest {
        // A deletion is stored as a tombstone with no value, and stays in the cache for the current customer even
        // after the backend has been told about it.
        val dimensions = provider(
            attribute("pending", null, isSynced = false),
            attribute("posted", null, isSynced = true),
            attribute("goal", "lose_weight"),
        ).dimensions(evaluationDate)

        assertThat(dimensions).containsOnlyKeys("goal")
    }

    @Test
    fun `an attribute set to an empty value is left out`() = runTest {
        // The SDK's other spelling of a deletion, and an empty string would compare equal to an absent value
        // anyway.
        val dimensions = provider(attribute("goal", ""), attribute("tier", "gold")).dimensions(evaluationDate)

        assertThat(dimensions).containsOnlyKeys("tier")
    }

    @Test
    fun `a customer with no attributes contributes no namespace at all`() = runTest {
        val values = resolver(provider()).snapshot().getOrThrow().values

        // An empty object is truthy in JSON Logic, so `{"var": "subscriberAttributes"}` would read as present for a
        // customer the app has never said anything about.
        assertThat(values).doesNotContainKey("subscriberAttributes")
    }

    @Test
    fun `attributes that cannot be read leave the other dimensions usable`() = runTest {
        // The read goes through the configured instance, which an app can tear down mid-evaluation.
        val failures = listOf(
            UninitializedPropertyAccessException("There is no singleton instance."),
            IllegalStateException("Something else entirely."),
        )

        for (failure in failures) {
            val snapshot = resolver(
                deviceProvider("platform" to "android"),
                SubscriberAttributesDimensionProvider { throw failure },
            ).snapshot()

            assertThat(snapshot.isSuccess).describedAs("%s", failure).isTrue()
            assertThat(snapshot.getOrThrow().values).describedAs("%s", failure).containsOnlyKeys("device")
        }
    }

    @Test
    fun `the attributes are read on every evaluation`() = runTest {
        var attributes = mapOf("goal" to attribute("goal", "lose_weight"))
        val provider = SubscriberAttributesDimensionProvider { attributes }

        assertThat(provider.dimensions(evaluationDate).valueOf("goal"))
            .isEqualTo(RulesDimensionValue.StringValue("lose_weight"))

        attributes = mapOf("goal" to attribute("goal", "gain_muscle"))

        assertThat(provider.dimensions(evaluationDate).valueOf("goal"))
            .isEqualTo(RulesDimensionValue.StringValue("gain_muscle"))
    }

    @Test
    fun `the attributes are readable by a predicate`() = runTest {
        val values = resolver(
            provider(
                attribute("\$email", "jane@example.com"),
                attribute("seats", "3"),
                attribute("goal", "lose_weight"),
                attribute("tier", "gold", setTime = recentSetDate),
            ),
        ).snapshot().getOrThrow().values

        val matching = listOf(
            """{"==": [{"var": "subscriberAttributes.${'$'}email.value"}, "jane@example.com"]}""",
            // The loose operators coerce, so a value the app set as a string is still comparable to a number.
            """{"==": [{"var": "subscriberAttributes.seats.value"}, 3]}""",
            """{">": [{"var": "subscriberAttributes.seats.value"}, 2]}""",
            // Each record carries the evaluation instant, so "set in the last 7 days" needs nothing else in scope.
            """{"<": [{"-": [{"var": "subscriberAttributes.tier.evaluatedAt"},
                {"var": "subscriberAttributes.tier.updatedAt"}]}, 604800000]}""",
        )
        val notMatching = listOf(
            """{"==": [{"var": "subscriberAttributes.goal.value"}, "gain_muscle"]}""",
            // An attribute the app never set on this device, and one the SDK does not expose. Both
            // need a default, since reading a name that resolves to nothing is an error.
            """{"!!": {"var": ["subscriberAttributes.favoriteColor.value", false]}}""",
            """{"!!": {"var": ["subscriberAttributes.goal.isSynced", false]}}""",
            // The same window, for an attribute set six weeks ago.
            """{"<": [{"-": [{"var": "subscriberAttributes.goal.evaluatedAt"},
                {"var": "subscriberAttributes.goal.updatedAt"}]}, 604800000]}""",
        )

        for (predicate in matching) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isTrue()
        }
        for (predicate in notMatching) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isFalse()
        }
    }

    private fun attribute(
        key: String,
        value: String?,
        isSynced: Boolean = true,
        setTime: Date = setDate,
    ) = SubscriberAttribute(key = key, value = value, setTime = setTime, isSynced = isSynced)

    private fun provider(vararg attributes: SubscriberAttribute) = SubscriberAttributesDimensionProvider {
        attributes.associateBy { attribute -> attribute.key.backendKey }
    }

    private fun resolver(vararg providers: RulesDimensionProvider) = RulesDimensionResolver(
        providers = providers.toList(),
        dateProvider = object : DateProvider {
            override val now: Date = evaluationDate
        },
    )

    private fun deviceProvider(vararg values: Pair<String, String>) = object : RulesDimensionProvider {
        override val namespace = RulesDimensionNamespace.Device
        override suspend fun dimensions(date: Date) =
            values.associate { (key, value) -> key to RulesDimensionValue.StringValue(value) }
    }

    private fun Map<String, RulesDimensionValue>.valueOf(name: String): RulesDimensionValue? =
        (this[name] as? RulesDimensionValue.ObjectValue)?.value?.get(KEY_VALUE)
}
