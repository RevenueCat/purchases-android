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
            mapOf(
                "device" to Value.ObjectValue(mapOf("appVersion" to Value.StringValue("1.2.3"))),
                "evaluatedAt" to Value.IntValue(evaluationDate.time),
            ),
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
                "evaluatedAt" to Value.IntValue(evaluationDate.time),
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
    fun `every provider sees the same evaluation date, and so does a predicate`() = runTest {
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
        assertThat(snapshot.values["evaluatedAt"]).isEqualTo(Value.IntValue(evaluationDate.time))
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

        assertThat(values).containsOnlyKeys("device", "evaluatedAt")
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

        assertThat(values).containsOnlyKeys("device", "evaluatedAt")
        // An empty object would be truthy, which is what makes the absence matter.
        assertThat(RulesEngine.evaluate("""{"!!": [{"var": "store"}]}""", values).getOrThrow()).isFalse()
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
    fun `a provider whose every name is unreachable leaves its namespace absent`() = runTest {
        val resolver = resolver(
            provider(RulesDimensionNamespace.Device, "platform" to string("android")),
            provider(RulesDimensionNamespace.SubscriberAttributes, "user.tier" to string("gold")),
        )

        val values = resolver.snapshot().getOrThrow().values

        // Filtering the names inside the namespace would leave an empty object behind, which is truthy.
        assertThat(values).containsOnlyKeys("device", "evaluatedAt")
        assertThat(
            RulesEngine.evaluate("""{"!!": [{"var": "subscriberAttributes"}]}""", values).getOrThrow(),
        ).isFalse()
    }

    @Test
    fun `no providers yields a scope of nothing but the evaluation instant`() = runTest {
        assertThat(resolver().snapshot().getOrThrow().values)
            .isEqualTo(mapOf("evaluatedAt" to Value.IntValue(evaluationDate.time)))
    }

    @Test
    fun `the evaluation instant is readable by a predicate at the root of the scope`() = runTest {
        val values = resolver(provider(RulesDimensionNamespace.Device, "platform" to string("android")))
            .snapshot()
            .getOrThrow()
            .values

        val matches = RulesEngine.evaluate("""{"==": [{"var": "evaluatedAt"}, ${evaluationDate.time}]}""", values)

        assertThat(matches.getOrThrow()).isTrue()
    }

    @Test
    fun `a collection the app user changed underneath is taken again`() = runTest {
        var appUserId = "before_login"
        var collections = 0
        val resolver = retryingResolver({ appUserId }) {
            collections++
            val collectedFor = appUserId
            // The app logs in during the first collection only.
            if (collections == 1) appUserId = "after_login"
            mapOf("platform" to string(collectedFor))
        }

        val values = resolver.snapshot().getOrThrow().values

        assertThat(collections).isEqualTo(2)
        // Reporting the first pass would describe a customer nobody is: these are the second pass's values,
        // collected for an app user that then stayed put.
        assertThat(values["device"])
            .isEqualTo(Value.ObjectValue(mapOf("platform" to Value.StringValue("after_login"))))
    }

    @Test
    fun `an app user that will not stay still fails the snapshot`() = runTest {
        var collections = 0
        // A different user on every read, so the retry cannot settle either.
        val resolver = retryingResolver({ "user_$collections" }) {
            collections++
            mapOf("platform" to string("android"))
        }

        val error = resolver.snapshot().exceptionOrNull()

        // Nothing here is worth reporting: an absence rule would match a customer whose purchases were simply
        // never read, which is indistinguishable from one who has none.
        assertThat(error).isEqualTo(RulesDimensionResolutionException.AppUserChanged)
        assertThat(collections).isEqualTo(2)
    }

    @Test
    fun `an app user that stays put is collected once`() = runTest {
        var collections = 0
        val resolver = retryingResolver({ "user_a" }) {
            collections++
            mapOf("platform" to string("android"))
        }

        assertThat(resolver.snapshot().isSuccess).isTrue()
        assertThat(collections).isEqualTo(1)
    }

    @Test
    fun `a provider that failed is not asked again`() = runTest {
        var collections = 0
        var appUserId = "before_login"
        val resolver = retryingResolver({ appUserId }) {
            collections++
            // Changing on top of the failure, so a retry would be visible if one happened.
            appUserId = "after_login"
            throw IllegalStateException("Nope.")
        }

        val error = resolver.snapshot().exceptionOrNull()

        // A provider that cannot produce its values is a configuration bug, and asking again does not fix it.
        assertThat(error).isInstanceOf(RulesDimensionResolutionException.ProviderFailed::class.java)
        assertThat(collections).isEqualTo(1)
    }

    @Test
    fun `the retry is taken against its own reference instant`() = runTest {
        val retryDate = Date(evaluationDate.time + 1_000)
        var appUserId = "before_login"
        var collections = 0
        val resolver = RulesDimensionResolver(
            providers = listOf(
                countingProvider {
                    collections++
                    if (collections == 1) appUserId = "after_login"
                    mapOf("platform" to string("android"))
                },
            ),
            dateProvider = object : DateProvider {
                override val now: Date get() = if (collections == 0) evaluationDate else retryDate
            },
            currentAppUserId = { appUserId },
        )

        val values = resolver.snapshot().getOrThrow().values

        // The instant describes the pass that actually produced these values, not the one that was discarded.
        assertThat(values["evaluatedAt"]).isEqualTo(Value.IntValue(retryDate.time))
    }

    private fun retryingResolver(
        currentAppUserId: () -> String,
        values: () -> Map<String, RulesDimensionValue>,
    ) = RulesDimensionResolver(
        providers = listOf(countingProvider(values)),
        dateProvider = object : DateProvider {
            override val now: Date = evaluationDate
        },
        currentAppUserId = currentAppUserId,
    )

    private fun countingProvider(values: () -> Map<String, RulesDimensionValue>) =
        object : RulesDimensionProvider {
            override val namespace = RulesDimensionNamespace.Device
            override suspend fun dimensions(date: Date) = values()
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
