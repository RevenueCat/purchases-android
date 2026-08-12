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
            provider(RulesDimensionNamespace.Device, "app_version" to string("1.2.3")),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).isEqualTo(
            mapOf("device" to Value.ObjectValue(mapOf("app_version" to Value.StringValue("1.2.3")))),
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
                ),
            ),
        )
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
