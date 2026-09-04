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
    private val evaluatedAt = "evaluated_at" to Value.IntValue(1_700_000_000_000)

    @Test
    fun `dimensions are merged into the root scope`() = runTest {
        val resolver = resolver(
            provider("app_version" to string("1.2.3")),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).isEqualTo(mapOf(evaluatedAt, "app_version" to Value.StringValue("1.2.3")))
    }

    @Test
    fun `dimensions are reachable by name from a predicate`() = runTest {
        val resolver = resolver(
            provider("platform" to string("android")),
        )
        val values = resolver.snapshot().getOrThrow().values

        val matches = RulesEngine.evaluate("""{"==": [{"var": "platform"}, "android"]}""", values)

        assertThat(matches.getOrThrow()).isTrue()
    }

    @Test
    fun `providers are merged`() = runTest {
        val resolver = resolver(
            provider("platform" to string("android")),
            provider("locale" to string("en-US")),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).isEqualTo(
            mapOf(
                evaluatedAt,
                "platform" to Value.StringValue("android"),
                "locale" to Value.StringValue("en-US"),
            ),
        )
    }

    @Test
    fun `two providers supplying the same dimension fail the snapshot`() = runTest {
        val resolver = resolver(
            provider("platform" to string("android")),
            provider("platform" to string("amazon")),
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("platform"))
    }

    @Test
    fun `a provider that throws fails the snapshot with its name`() = runTest {
        val resolver = resolver(
            provider("storefront" to string("USA")),
            object : RulesDimensionProvider {
                override val name = "device"
                override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
                    throw IllegalStateException("nope")
            },
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error)
            .isEqualTo(RulesDimensionResolutionException.ProviderFailed("device", "nope"))
    }

    @Test
    fun `cancellation propagates instead of failing the snapshot`() = runTest {
        val resolver = resolver(
            object : RulesDimensionProvider {
                override val name = "device"
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
                override val name = "device"
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
    fun `every snapshot carries the evaluation instant`() = runTest {
        val values = resolver().snapshot().getOrThrow().values

        assertThat(values).isEqualTo(mapOf(evaluatedAt))
    }

    @Test
    fun `the evaluation instant is ordered by a predicate`() = runTest {
        val values = resolver().snapshot().getOrThrow().values

        assertThat(
            RulesEngine.evaluate("""{">": [{"var": "evaluated_at"}, 1699999999999]}""", values).getOrThrow(),
        ).isTrue()
        assertThat(
            RulesEngine.evaluate("""{">": [{"var": "evaluated_at"}, 1700000000001]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `a provider supplying the evaluation instant fails the snapshot`() = runTest {
        val resolver = resolver(
            provider("evaluated_at" to RulesDimensionValue.DateValue(evaluationDate)),
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("evaluated_at"))
    }

    @Test
    fun `each dimension value maps to its engine counterpart`() = runTest {
        val resolver = resolver(
            provider(
                "text" to RulesDimensionValue.StringValue("value"),
                "flag" to RulesDimensionValue.BoolValue(true),
                "count" to RulesDimensionValue.IntValue(3),
                "ratio" to RulesDimensionValue.DoubleValue(1.5),
                "date" to RulesDimensionValue.DateValue(evaluationDate),
                "nothing" to RulesDimensionValue.NullValue,
                "records" to RulesDimensionValue.ObjectListValue(
                    listOf(
                        mapOf(
                            "id" to RulesDimensionValue.StringValue("one"),
                            "nothing" to RulesDimensionValue.NullValue,
                        ),
                    ),
                ),
                "record" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "id" to RulesDimensionValue.StringValue("one"),
                        "nothing" to RulesDimensionValue.NullValue,
                    ),
                ),
            ),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).isEqualTo(
            mapOf(
                evaluatedAt,
                "text" to Value.StringValue("value"),
                "flag" to Value.BoolValue(true),
                "count" to Value.IntValue(3),
                "ratio" to Value.FloatValue(1.5),
                "date" to Value.IntValue(1_700_000_000_000),
                "nothing" to Value.Null,
                "records" to Value.ArrayValue(
                    listOf(
                        Value.ObjectValue(mapOf("id" to Value.StringValue("one"), "nothing" to Value.Null)),
                    ),
                ),
                "record" to Value.ObjectValue(mapOf("id" to Value.StringValue("one"), "nothing" to Value.Null)),
            ),
        )
    }

    @Test
    fun `a date dimension is ordered by a predicate`() = runTest {
        val resolver = resolver(
            provider(
                "expires_at" to RulesDimensionValue.DateValue(Date(1_700_000_000_000)),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        assertThat(
            RulesEngine.evaluate("""{">": [{"var": "expires_at"}, 1699999999999]}""", values).getOrThrow(),
        ).isTrue()
        assertThat(
            RulesEngine.evaluate("""{">": [{"var": "expires_at"}, 1700000000001]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `an object list dimension is walked one record at a time by a predicate`() = runTest {
        val resolver = resolver(
            provider(
                "purchases" to RulesDimensionValue.ObjectListValue(
                    listOf(
                        mapOf(
                            "product_id" to RulesDimensionValue.StringValue("plus"),
                            "is_active" to RulesDimensionValue.BoolValue(false),
                        ),
                        mapOf(
                            "product_id" to RulesDimensionValue.StringValue("pro"),
                            "is_active" to RulesDimensionValue.BoolValue(true),
                        ),
                    ),
                ),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        val active = """{"some": [{"var": "purchases"},
            {"and": [{"==": [{"var": "product_id"}, "pro"]}, {"var": "is_active"}]}]}"""
        assertThat(RulesEngine.evaluate(active, values).getOrThrow()).isTrue()

        // A record is searched on its own values, so the same product with the other record's state does not match.
        val inactive = """{"some": [{"var": "purchases"},
            {"and": [{"==": [{"var": "product_id"}, "plus"]}, {"var": "is_active"}]}]}"""
        assertThat(RulesEngine.evaluate(inactive, values).getOrThrow()).isFalse()

        // A record is reachable by index too.
        val byIndex = """{"==": [{"var": "purchases.1.product_id"}, "pro"]}"""
        assertThat(RulesEngine.evaluate(byIndex, values).getOrThrow()).isTrue()
    }

    @Test
    fun `an empty object list is an empty array rather than an absent dimension`() = runTest {
        val resolver = resolver(
            provider(
                "purchases" to RulesDimensionValue.ObjectListValue(emptyList()),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        // "Has bought nothing" has to be a definite answer, which only a present, empty array gives.
        assertThat(values["purchases"]).isEqualTo(Value.ArrayValue(emptyList()))
        assertThat(
            RulesEngine.evaluate("""{"none": [{"var": "purchases"}, {"var": "is_active"}]}""", values)
                .getOrThrow(),
        ).isTrue()
    }

    @Test
    fun `an object dimension is read through by dot-path rather than searched`() = runTest {
        val resolver = resolver(
            provider(
                "goal" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "value" to RulesDimensionValue.StringValue("lose_weight"),
                        "updated_at" to RulesDimensionValue.DateValue(Date(1_700_000_000_000)),
                    ),
                ),
            ),
        )
        val values = resolver.snapshot().getOrThrow().values

        val byName = """{"==": [{"var": "goal.value"}, "lose_weight"]}"""
        assertThat(RulesEngine.evaluate(byName, values).getOrThrow()).isTrue()

        // Unlike a record inside an object list, a predicate reading one of these still sees the scope around it,
        // because no iteration operator is involved.
        val alongsideTheRestOfTheScope = """{"and": [{"==": [{"var": "goal.value"}, "lose_weight"]},
            {">": [{"var": "goal.updated_at"}, 1699999999999]}]}"""
        assertThat(RulesEngine.evaluate(alongsideTheRestOfTheScope, values).getOrThrow()).isTrue()
    }

    @Test
    fun `custom variables are nested under the custom root`() = runTest {
        val resolver = resolver(provider("platform" to string("android")))

        val values = resolver.snapshot(mapOf("source" to string("settings"))).getOrThrow().values

        val matches = RulesEngine.evaluate("""{"==": [{"var": "custom.source"}, "settings"]}""", values)
        assertThat(matches.getOrThrow()).isTrue()
    }

    @Test
    fun `no custom variables leaves the root absent rather than empty`() = runTest {
        val resolver = resolver(provider("platform" to string("android")))

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("evaluated_at", "platform")
        // An empty object would be truthy, so an absent root is what makes this a non-match.
        // The default keeps the read legal, because an unresolved name is an error.
        assertThat(
            RulesEngine.evaluate("""{"!!": [{"var": ["custom", false]}]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `a custom variable no predicate could read is dropped rather than exposed`() = runTest {
        val resolver = resolver()

        val values = resolver
            .snapshot(mapOf("user.tier" to string("gold"), "tier" to string("gold")))
            .getOrThrow()
            .values

        assertThat(values["custom"]).isEqualTo(Value.ObjectValue(mapOf("tier" to Value.StringValue("gold"))))
    }

    @Test
    fun `backend values are nested under the backend root`() = runTest {
        val resolver = resolver(provider("platform" to string("android")))

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
    fun `no backend values leaves the root absent rather than empty`() = runTest {
        val resolver = resolver(provider("platform" to string("android")))

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("evaluated_at", "platform")
        // An absent hash reads as the rule's default, which is what keeps unknown hashes forward-compatible.
        assertThat(
            RulesEngine.evaluate("""{"var": ["backend.unknown_hash", false]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `a provider claiming the backend root fails the snapshot`() = runTest {
        val resolver = resolver(
            provider("backend" to RulesDimensionValue.ObjectValue(mapOf("source" to string("x")))),
        )

        // Reserved whether or not this evaluation supplies backend values, so the collision is deterministic.
        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("backend"))
    }

    @Test
    fun `a provider claiming the custom root fails the snapshot`() = runTest {
        val resolver = resolver(
            provider("custom" to RulesDimensionValue.ObjectValue(mapOf("source" to string("x")))),
        )

        val error = resolver.snapshot().exceptionOrNull()

        assertThat(error).isEqualTo(RulesDimensionResolutionException.ConflictingDimension("custom"))
    }

    @Test
    fun `a provider with nothing to contribute adds nothing to the scope`() = runTest {
        val resolver = resolver(
            provider("platform" to string("android")),
            provider(),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("evaluated_at", "platform")
    }

    @Test
    fun `a name no predicate could read is dropped rather than exposed`() = runTest {
        val resolver = resolver(
            provider(
                // A '.' would be walked as a path through a "user" object that does not exist, and a blank name is
                // not one a predicate can be written against.
                "user.tier" to string("gold"),
                "" to string("anything"),
                " " to string("anything"),
                "tier" to string("gold"),
            ),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).containsOnlyKeys("evaluated_at", "tier")
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
                        " " to string("anything"),
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
    fun `an object dimension with nothing readable is absent rather than empty`() = runTest {
        // An empty object is truthy in JSON Logic, so keeping one would make `{"var": "profile"}` read as present.
        // Same whether the object was supplied empty or every name inside it was unreachable, at any depth.
        val resolver = resolver(
            provider(
                "empty" to RulesDimensionValue.ObjectValue(emptyMap()),
                "profile" to RulesDimensionValue.ObjectValue(mapOf("user.tier" to string("gold"))),
                "goal" to RulesDimensionValue.ObjectValue(
                    mapOf(
                        "value" to string("lose_weight"),
                        "emptied" to RulesDimensionValue.ObjectValue(mapOf("also.bad" to string("dropped"))),
                    ),
                ),
            ),
        )

        val values = resolver
            .snapshot(mapOf("settings" to RulesDimensionValue.ObjectValue(emptyMap())))
            .getOrThrow()
            .values

        assertThat(values).containsOnlyKeys("evaluated_at", "goal")
        assertThat(values["goal"]).isEqualTo(Value.ObjectValue(mapOf("value" to Value.StringValue("lose_weight"))))
        assertThat(
            RulesEngine.evaluate("""{"!!": [{"var": ["profile", false]}]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `an object list record with nothing readable is dropped from the array`() = runTest {
        val resolver = resolver(
            provider(
                "results" to RulesDimensionValue.ObjectListValue(
                    listOf(
                        mapOf("user.tier" to string("gold")),
                        mapOf("id" to string("b")),
                    ),
                ),
            ),
        )

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values["results"]).isEqualTo(
            Value.ArrayValue(listOf(Value.ObjectValue(mapOf("id" to Value.StringValue("b"))))),
        )
    }

    @Test
    fun `no providers yields a scope with only the evaluation instant`() = runTest {
        assertThat(resolver().snapshot().getOrThrow().values).isEqualTo(mapOf(evaluatedAt))
    }

    private fun resolver(vararg providers: RulesDimensionProvider) = RulesDimensionResolver(
        providers = providers.toList(),
        dateProvider = object : DateProvider {
            override val now: Date = evaluationDate
        },
    )

    private fun string(value: String) = RulesDimensionValue.StringValue(value)

    private fun provider(
        vararg values: Pair<String, RulesDimensionValue>,
        providerName: String = "test",
    ) = object : RulesDimensionProvider {
        override val name = providerName
        override suspend fun dimensions(date: Date) = values.toMap()
    }
}
