@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class RulesDimensionResolverTest {

    private val evaluationDate = Date(1_700_000_000_000)

    @Test
    fun `dimensions are nested under their provider's namespace`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "appVersion" to string("1.2.3")),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).isEqualTo(
            mapOf("device" to Value.ObjectValue(mapOf("appVersion" to Value.StringValue("1.2.3")))),
        )
    }

    @Test
    fun `nested dimensions are reachable by dot-path from a predicate`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
        )
        val values = resolver.snapshot().getOrThrow().values

        val matches = RulesEngine.evaluate("""{"==": [{"var": "device.platform"}, "android"]}""", values)

        assertThat(matches.getOrThrow()).isTrue()
    }

    @Test
    fun `providers sharing a namespace are merged`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
            provider(RulesDimensionNamespace.Device, "locale" to string("en-US")),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).isEqualTo(
            mapOf(
                "device" to Value.ObjectValue(
                    mapOf(
                        "platform" to Value.StringValue("android"),
                        "locale" to Value.StringValue("en-US"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `two providers supplying the same dimension fail the snapshot`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
            provider(RulesDimensionNamespace.Device, "platform" to string("amazon")),
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("device.platform"))
    }

    @Test
    fun `a provider that throws fails the snapshot with its identifier`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
            object : RulesDimensionProvider {
                override val identifier = "failing"
                override val namespace = RulesDimensionNamespace.Device
                override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
                    throw IllegalStateException("nope")
            },
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ProviderFailed("failing", "nope"))
    }

    @Test
    fun `cancellation propagates instead of failing the snapshot`() = runTest {
        val resolver = resolver(
            object : RulesDimensionProvider {
                override val identifier = "cancelling"
                override val namespace = RulesDimensionNamespace.Device
                override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
                    throw CancellationException("cancelled")
            },
        )

        val thrown = try {
            resolver.snapshot()
            null
        } catch (e: CancellationException) {
            e
        }

        assertThat(thrown).hasMessage("cancelled")
    }

    @Test
    fun `every provider sees the same evaluation date`() = runTest {
        val dates = mutableListOf<Date>()
        val recordingProvider = { name: String ->
            object : RulesDimensionProvider {
                override val identifier = name
                override val namespace = RulesDimensionNamespace.Device
                override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
                    dates += date
                    return emptyMap()
                }
            }
        }
        val resolver = resolver(recordingProvider("first"), recordingProvider("second"))

        val snapshot = resolver.snapshot().getOrThrow()

        assertThat(dates).containsExactly(evaluationDate, evaluationDate)
        assertThat(snapshot.evaluationDate).isEqualTo(evaluationDate)
    }

    @Test
    fun `each dimension value maps to its engine counterpart`() = runTest {
        val resolver = resolver(
            provider(
                RulesDimensionNamespace.Device,
                "text" to RulesDimensionValue.StringValue("value"),
                "flag" to RulesDimensionValue.BoolValue(true),
                "count" to RulesDimensionValue.IntValue(3),
                "ratio" to RulesDimensionValue.DoubleValue(1.5),
                "date" to RulesDimensionValue.DateValue(evaluationDate),
                "records" to RulesDimensionValue.ObjectListValue(
                    listOf(mapOf("id" to RulesDimensionValue.StringValue("one"))),
                ),
            ),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values["device"]).isEqualTo(
            Value.ObjectValue(
                mapOf(
                    "text" to Value.StringValue("value"),
                    "flag" to Value.BoolValue(true),
                    "count" to Value.IntValue(3),
                    "ratio" to Value.FloatValue(1.5),
                    "date" to Value.IntValue(1_700_000_000_000),
                    "records" to Value.ArrayValue(
                        listOf(Value.ObjectValue(mapOf("id" to Value.StringValue("one")))),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `a date dimension is ordered by a predicate`() = runTest {
        val resolver = resolver(
            provider(
                RulesDimensionNamespace.Device,
                "expiresAt" to RulesDimensionValue.DateValue(Date(1_700_000_000_000)),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        assertThat(
            RulesEngine.evaluate("""{">": [{"var": "device.expiresAt"}, 1699999999999]}""", values).getOrThrow(),
        ).isTrue()
        assertThat(
            RulesEngine.evaluate("""{">": [{"var": "device.expiresAt"}, 1700000000001]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `an object list dimension is walked one record at a time by a predicate`() = runTest {
        val resolver = resolver(
            provider(
                RulesDimensionNamespace.Device,
                "purchases" to RulesDimensionValue.ObjectListValue(
                    listOf(
                        mapOf(
                            "productId" to RulesDimensionValue.StringValue("plus"),
                            "isActive" to RulesDimensionValue.BoolValue(false),
                        ),
                        mapOf(
                            "productId" to RulesDimensionValue.StringValue("pro"),
                            "isActive" to RulesDimensionValue.BoolValue(true),
                        ),
                    ),
                ),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        val active = """{"some": [{"var": "device.purchases"},
            {"and": [{"==": [{"var": "productId"}, "pro"]}, {"var": "isActive"}]}]}"""
        assertThat(RulesEngine.evaluate(active, values).getOrThrow()).isTrue()

        // A record is searched on its own values, so the same product with the other record's state does not match.
        val inactive = """{"some": [{"var": "device.purchases"},
            {"and": [{"==": [{"var": "productId"}, "plus"]}, {"var": "isActive"}]}]}"""
        assertThat(RulesEngine.evaluate(inactive, values).getOrThrow()).isFalse()

        // A record is reachable by index too.
        val byIndex = """{"==": [{"var": "device.purchases.1.productId"}, "pro"]}"""
        assertThat(RulesEngine.evaluate(byIndex, values).getOrThrow()).isTrue()
    }

    @Test
    fun `an empty object list is an empty array rather than an absent dimension`() = runTest {
        val resolver = resolver(
            provider(
                RulesDimensionNamespace.Device,
                "purchases" to RulesDimensionValue.ObjectListValue(emptyList()),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        // "Has bought nothing" has to be a definite answer, which only a present, empty array gives.
        assertThat(values["device"]).isEqualTo(Value.ObjectValue(mapOf("purchases" to Value.ArrayValue(emptyList()))))
        assertThat(
            RulesEngine.evaluate("""{"none": [{"var": "device.purchases"}, {"var": "isActive"}]}""", values)
                .getOrThrow(),
        ).isTrue()
    }

    @Test
    fun `custom variables are nested under the custom namespace`() = runTest {
        val resolver = resolver(provider(RulesDimensionNamespace.Device, "platform" to string("android")))

        val values = resolver.snapshot(mapOf("source" to string("settings"))).getOrThrow().values

        val matches = RulesEngine.evaluate("""{"==": [{"var": "custom.source"}, "settings"]}""", values)
        assertThat(matches.getOrThrow()).isTrue()
    }

    @Test
    fun `no custom variables leaves the namespace absent rather than empty`() = runTest {
        val resolver = resolver(provider(RulesDimensionNamespace.Device, "platform" to string("android")))

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("device")
        // An empty object would be truthy, so an absent namespace is what makes this a non-match.
        assertThat(RulesEngine.evaluate("""{"!!": [{"var": "custom"}]}""", values).getOrThrow()).isFalse()
    }

    @Test
    fun `a custom variable colliding with a provider fails the snapshot`() = runTest {
        val resolver = resolver(provider(RulesDimensionNamespace.Custom, "source" to string("provided")))

        val error = resolver.snapshot(mapOf("source" to string("call"))).exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("custom.source"))
    }

    @Test
    fun `a provider with nothing to contribute leaves its namespace absent`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
            provider(RulesDimensionNamespace.Store),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("device")
        // An empty object would be truthy, which is what makes the absence matter.
        assertThat(RulesEngine.evaluate("""{"!!": [{"var": "store"}]}""", values).getOrThrow()).isFalse()
    }

    @Test
    fun `no providers yields an empty scope`() = runTest {
        assertThat(resolver().snapshot().getOrThrow().values).isEmpty()
    }

    private fun resolver(vararg providers: RulesDimensionProvider) = RulesDimensionResolver(
        providers = providers.toList(),
        dateProvider = object : DateProvider {
            override val now: Date = evaluationDate
        },
    )

    private fun string(value: String) = RulesDimensionValue.StringValue(value)

    private fun provider(
        dimensionNamespace: RulesDimensionNamespace,
        vararg values: Pair<String, RulesDimensionValue>,
    ) = object : RulesDimensionProvider {
        override val identifier = dimensionNamespace.key
        override val namespace = dimensionNamespace
        override suspend fun dimensions(date: Date) = values.toMap()
    }
}
