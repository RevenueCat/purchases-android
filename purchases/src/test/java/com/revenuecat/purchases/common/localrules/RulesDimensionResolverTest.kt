@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

// Robolectric because the resolver warns about a dimension it drops, and warnLog reaches android.util.Log.
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
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
    fun `a provider that throws fails the snapshot with its namespace`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Store, "country" to string("USA")),
            object : RulesDimensionProvider {
                override val namespace = RulesDimensionNamespace.Device
                override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
                    throw IllegalStateException("nope")
            },
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error)
            .isEqualTo(RulesDimensionResolutionException.ProviderFailed(RulesDimensionNamespace.Device, "nope"))
    }

    @Test
    fun `cancellation propagates instead of failing the snapshot`() = runTest {
        val resolver = resolver(
            object : RulesDimensionProvider {
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
        val recordingProvider = {
            object : RulesDimensionProvider {
                override val namespace = RulesDimensionNamespace.Device
                override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> {
                    dates += date
                    return emptyMap()
                }
            }
        }
        val resolver = resolver(recordingProvider(), recordingProvider())

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
                "record" to RulesDimensionValue.ObjectValue(
                    mapOf("id" to RulesDimensionValue.StringValue("one")),
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
                    "record" to Value.ObjectValue(mapOf("id" to Value.StringValue("one"))),
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
    fun `an object dimension is read through by name rather than searched`() = runTest {
        val resolver = resolver(
            provider(
                RulesDimensionNamespace.Device,
                "goal" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "value" to RulesDimensionValue.StringValue("lose_weight"),
                        "updatedAt" to RulesDimensionValue.DateValue(Date(1_700_000_000_000)),
                    ),
                ),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        val byName = """{"==": [{"var": "device.goal.value"}, "lose_weight"]}"""
        assertThat(RulesEngine.evaluate(byName, values).getOrThrow()).isTrue()

        // Unlike a record inside an object list, a predicate reading one of these still sees the scope around it,
        // because no iteration operator is involved.
        val alongsideTheRestOfTheScope = """{"and": [{"==": [{"var": "device.goal.value"}, "lose_weight"]},
            {">": [{"var": "device.goal.updatedAt"}, 1699999999999]}]}"""
        assertThat(RulesEngine.evaluate(alongsideTheRestOfTheScope, values).getOrThrow()).isTrue()
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
        // The default keeps the read legal, because an unresolved name is an error.
        assertThat(
            RulesEngine.evaluate("""{"!!": [{"var": ["custom", false]}]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `backend values are nested under the backend namespace`() = runTest {
        val resolver = resolver(provider(RulesDimensionNamespace.Device, "platform" to string("android")))

        val values = resolver
            .snapshot(backendValues = mapOf("349OzehoTyCAdiZblj9w0J0yD-Uow8X3" to RulesDimensionValue.BoolValue(true)))
            .getOrThrow()
            .values

        val matches = RulesEngine.evaluate(
            """{"var": ["backend.349OzehoTyCAdiZblj9w0J0yD-Uow8X3", false]}""",
            values,
        )
        assertThat(matches.getOrThrow()).isTrue()
    }

    @Test
    fun `no backend values leaves the namespace absent rather than empty`() = runTest {
        val resolver = resolver(provider(RulesDimensionNamespace.Device, "platform" to string("android")))

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("device")
        // An absent hash reads as the rule's default, which is what keeps unknown hashes forward-compatible.
        assertThat(
            RulesEngine.evaluate("""{"var": ["backend.unknown_hash", false]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `a backend value colliding with a provider fails the snapshot`() = runTest {
        val resolver = resolver(provider(RulesDimensionNamespace.Backend, "hash" to RulesDimensionValue.BoolValue(false)))

        val error = resolver
            .snapshot(backendValues = mapOf("hash" to RulesDimensionValue.BoolValue(true)))
            .exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("backend.hash"))
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
        assertThat(
            RulesEngine.evaluate("""{"!!": [{"var": ["store", false]}]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `a name no predicate could read is dropped rather than exposed`() = runTest {
        val resolver = resolver(
            provider(
                RulesDimensionNamespace.SubscriberAttributes,
                // A '.' would be walked as a path through a "user" object that does not exist, and "" is not a
                // name a predicate can be written against.
                "user.tier" to string("gold"),
                "" to string("anything"),
                "tier" to string("gold"),
            ),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values["subscriberAttributes"])
            .isEqualTo(Value.ObjectValue(mapOf("tier" to Value.StringValue("gold"))))
    }

    @Test
    fun `a nested name no predicate could read is dropped rather than exposed`() = runTest {
        // Object values carry names too — one `var` path segment each — and the backend's pre-evaluated results
        // are the source that can nest arbitrarily, so the same reachability rule applies at every depth.
        val values = resolver().snapshot(
            backendValues = mapOf(
                "profile" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "user.tier" to string("gold"),
                        "" to string("anything"),
                        "tier" to string("gold"),
                        "nested" to RulesDimensionValue.ObjectValue(
                            mapOf("also.bad" to string("dropped"), "kept" to string("yes")),
                        ),
                    ),
                ),
            ),
        ).getOrThrow().values

        assertThat(values["backend"]).isEqualTo(
            Value.ObjectValue(
                mapOf(
                    "profile" to Value.ObjectValue(
                        mapOf(
                            "tier" to Value.StringValue("gold"),
                            "nested" to Value.ObjectValue(mapOf("kept" to Value.StringValue("yes"))),
                        ),
                    ),
                ),
            ),
        )
        assertThat(
            RulesEngine.evaluate("""{"==": [{"var": "backend.profile.tier"}, "gold"]}""", values).getOrThrow(),
        ).isTrue()
    }

    @Test
    fun `a name inside an object list record no predicate could read is dropped rather than exposed`() = runTest {
        val values = resolver().snapshot(
            backendValues = mapOf(
                "results" to RulesDimensionValue.ObjectListValue(
                    listOf(
                        mapOf("user.tier" to string("gold"), "id" to string("a")),
                        mapOf("id" to string("b")),
                    ),
                ),
            ),
        ).getOrThrow().values

        assertThat(values["backend"]).isEqualTo(
            Value.ObjectValue(
                mapOf(
                    "results" to Value.ArrayValue(
                        listOf(
                            Value.ObjectValue(mapOf("id" to Value.StringValue("a"))),
                            Value.ObjectValue(mapOf("id" to Value.StringValue("b"))),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `a provider whose every name is unreachable leaves its namespace absent`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
            provider(RulesDimensionNamespace.SubscriberAttributes, "user.tier" to string("gold")),
        )

        val values = resolver.snapshot().getOrThrow().values

        // Filtering the names inside the namespace would leave an empty object behind, which is truthy.
        assertThat(values).containsOnlyKeys("device")
        assertThat(
            RulesEngine.evaluate("""{"!!": [{"var": ["subscriberAttributes", false]}]}""", values).getOrThrow(),
        ).isFalse()
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
        override val namespace = dimensionNamespace
        override suspend fun dimensions(date: Date) = values.toMap()
    }
}
