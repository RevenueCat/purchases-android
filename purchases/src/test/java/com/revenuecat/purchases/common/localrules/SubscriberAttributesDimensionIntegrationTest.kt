@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider.Companion.KEY_EVALUATED_AT
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider.Companion.KEY_UPDATED_AT
import com.revenuecat.purchases.common.localrules.SubscriberAttributesDimensionProvider.Companion.KEY_VALUE
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

/**
 * End-to-end from `setAttributes` to a predicate: drives the real [SubscriberAttributesManager] →
 * [SubscriberAttributesCache] → [DeviceCache] → real `SharedPreferences` → [RulesDimensionResolver] →
 * [RulesEngine], wired the way `PurchasesFactory` wires them. Only the poster and the device-identifier fetcher
 * are faked, and `setAttributes` never reaches either.
 *
 * The unit tests around [SubscriberAttributesDimensionProvider] hand it attributes directly, so nothing there
 * covers the parts that can silently go wrong between an app calling `setAttributes` and a rule reading the
 * result: the cache key, the round-trip through JSON on disk, and which app user ID the attributes are keyed on.
 *
 * It does not prove `PurchasesFactory` passes the identity manager rather than something else — that would mean
 * configuring an SDK instance. What it proves is that reading the app user ID out of the same [DeviceCache] the
 * attributes were written against is enough to find them.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberAttributesDimensionIntegrationTest {

    private val evaluationDate = Date(1_718_452_800_000)

    private lateinit var cache: DeviceCache
    private lateinit var attributesCache: SubscriberAttributesCache
    private lateinit var attributes: SubscriberAttributesManager
    private lateinit var resolver: RulesDimensionResolver

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("subscriber_attributes_dimension_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        cache = DeviceCache(preferences, apiKey = "api_key")
        attributesCache = SubscriberAttributesCache(cache)
        attributes = SubscriberAttributesManager(
            attributesCache,
            backend = mockk(),
            deviceIdentifiersFetcher = mockk(),
            automaticDeviceIdentifierCollectionEnabled = false,
        )
        resolver = RulesDimensionResolver(
            providers = listOf(
                // The same expression PurchasesFactory builds, with the app user ID read from the cache the
                // identity manager reads it from.
                SubscriberAttributesDimensionProvider {
                    attributesCache.getAllStoredSubscriberAttributes(cache.getCachedAppUserID() ?: "")
                },
            ),
            dateProvider = object : DateProvider {
                override val now: Date get() = evaluationDate
            },
        )
    }

    @Test
    fun `an attribute the app set is readable by a predicate`() = runTest {
        val before = Date()
        cache.cacheAppUserID("user_a")
        attributes.setAttributes(mapOf("goal" to "lose_weight", "seats" to "3"), "user_a")

        val values = resolver.snapshot().getOrThrow().values

        val goal = values.recordFor("goal")
        assertThat(goal[KEY_VALUE]).isEqualTo(Value.StringValue("lose_weight"))
        assertThat(goal[KEY_EVALUATED_AT]).isEqualTo(Value.IntValue(evaluationDate.time))
        // Set by this device just now, so it survived the round-trip through JSON with the value.
        assertThat((goal[KEY_UPDATED_AT] as Value.IntValue).value).isBetween(before.time, Date().time)

        assertThat(
            RulesEngine.evaluate(
                """{"and": [{"==": [{"var": "subscriberAttributes.goal.value"}, "lose_weight"]},
                    {"==": [{"var": "subscriberAttributes.seats.value"}, 3]}]}""",
                values,
            ).getOrThrow(),
        ).isTrue()
    }

    @Test
    fun `a reserved attribute keeps its name all the way to the predicate`() = runTest {
        cache.cacheAppUserID("user_a")
        attributes.setAttribute(SubscriberAttributeKey.Email, "jane@example.com", "user_a")

        val values = resolver.snapshot().getOrThrow().values

        assertThat(
            RulesEngine.evaluate(
                """{"==": [{"var": "subscriberAttributes.${'$'}email.value"}, "jane@example.com"]}""",
                values,
            ).getOrThrow(),
        ).isTrue()
    }

    @Test
    fun `only the current customer's attributes are readable`() = runTest {
        attributes.setAttributes(mapOf("goal" to "lose_weight"), "user_a")
        attributes.setAttributes(mapOf("goal" to "gain_muscle"), "user_b")

        cache.cacheAppUserID("user_a")
        assertThat(resolver.snapshot().getOrThrow().values.recordFor("goal")[KEY_VALUE])
            .isEqualTo(Value.StringValue("lose_weight"))

        cache.cacheAppUserID("user_b")
        assertThat(resolver.snapshot().getOrThrow().values.recordFor("goal")[KEY_VALUE])
            .isEqualTo(Value.StringValue("gain_muscle"))
    }

    @Test
    fun `a customer the app has said nothing about contributes no namespace`() = runTest {
        attributes.setAttributes(mapOf("goal" to "lose_weight"), "user_a")
        cache.cacheAppUserID("user_b")

        val values = resolver.snapshot().getOrThrow().values

        assertThat(values).doesNotContainKey("subscriberAttributes")
    }

    @Test
    fun `a deleted attribute stops being readable while its tombstone is still stored`() = runTest {
        cache.cacheAppUserID("user_a")
        attributes.setAttributes(mapOf("goal" to "lose_weight", "tier" to "gold"), "user_a")

        attributes.setAttributes(mapOf("goal" to null), "user_a")

        val values = resolver.snapshot().getOrThrow().values
        assertThat((values["subscriberAttributes"] as Value.ObjectValue).entries).containsOnlyKeys("tier")
        // The deletion is still on disk waiting to be posted; it is the scope that leaves it out, not the cache.
        assertThat(attributesCache.getAllStoredSubscriberAttributes("user_a")).containsKey("goal")
    }

    @Test
    fun `an attribute a predicate could not reach never makes it into the scope`() = runTest {
        cache.cacheAppUserID("user_a")
        attributes.setAttributes(mapOf("user.tier" to "gold", "tier" to "gold"), "user_a")

        val values = resolver.snapshot().getOrThrow().values

        assertThat((values["subscriberAttributes"] as Value.ObjectValue).entries).containsOnlyKeys("tier")
    }

    private fun Map<String, Value>.recordFor(name: String): Map<String, Value> =
        ((this["subscriberAttributes"] as Value.ObjectValue).entries[name] as Value.ObjectValue).entries
}
