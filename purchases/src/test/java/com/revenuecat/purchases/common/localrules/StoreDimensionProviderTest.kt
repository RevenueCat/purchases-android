@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.rules.RulesEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class StoreDimensionProviderTest {

    private val date = Date(1_700_000_000_000)

    @Test
    fun `the store country is exposed as a three-letter code`() = runTest {
        assertThat(provider("US").dimensions(date))
            .isEqualTo(mapOf("country" to RulesDimensionValue.StringValue("USA")))
        assertThat(provider("ES").dimensions(date))
            .isEqualTo(mapOf("country" to RulesDimensionValue.StringValue("ESP")))
    }

    @Test
    fun `an unknown store country contributes nothing`() = runTest {
        // "UK" is what the Amazon Appstore reports for the United Kingdom, and it is not an ISO region. Region
        // subtags are two letters, so a store already reporting an alpha-3 code would be dropped too.
        for (countryCode in listOf(null, "", "UK", "ZZ", "abc", "1", "USA")) {
            assertThat(provider(countryCode).dimensions(date))
                .describedAs("country code '%s'", countryCode)
                .isEmpty()
        }
    }

    @Test
    fun `a store that cannot answer leaves the other dimensions usable`() = runTest {
        // Not just PurchasesException: the fetch goes through the configured instance, which an app can tear down
        // mid-evaluation, and the store is a third party.
        val failures = listOf(
            PurchasesException(PurchasesError(PurchasesErrorCode.StoreProblemError, "Nope.")),
            UninitializedPropertyAccessException("There is no singleton instance."),
            IllegalStateException("Something else entirely."),
        )

        for (failure in failures) {
            val snapshot = RulesDimensionResolver(
                providers = listOf(
                    provider(RulesDimensionNamespace.Device, "platform" to "android"),
                    StoreDimensionProvider { throw failure },
                ),
            ).snapshot()

            assertThat(snapshot.isSuccess).describedAs("%s", failure).isTrue()
            assertThat(snapshot.getOrThrow().values)
                .describedAs("%s", failure)
                .containsOnlyKeys("device")
        }
    }

    @Test
    fun `cancellation while fetching the store country propagates`() = runTest {
        val cancelling = StoreDimensionProvider { throw CancellationException("cancelled") }

        val thrown = try {
            cancelling.dimensions(date)
            null
        } catch (e: CancellationException) {
            e
        }

        assertThat(thrown).hasMessage("cancelled")
    }

    @Test
    fun `the store country is fetched on every evaluation`() = runTest {
        var countryCode: String? = "US"
        val provider = StoreDimensionProvider { countryCode }

        assertThat(provider.dimensions(date)["country"]).isEqualTo(RulesDimensionValue.StringValue("USA"))

        countryCode = "ES"

        assertThat(provider.dimensions(date)["country"]).isEqualTo(RulesDimensionValue.StringValue("ESP"))
    }

    @Test
    fun `the store country is reachable by dot-path from a predicate`() = runTest {
        val values = RulesDimensionResolver(providers = listOf(provider("US"))).snapshot().getOrThrow().values

        val matches = RulesEngine.evaluate("""{"==": [{"var": "store.country"}, "USA"]}""", values)

        assertThat(matches.getOrThrow()).isTrue()
    }

    private fun provider(countryCode: String?) = StoreDimensionProvider { countryCode }

    private fun provider(
        dimensionNamespace: RulesDimensionNamespace,
        vararg values: Pair<String, String>,
    ) = object : RulesDimensionProvider {
        override val namespace = dimensionNamespace
        override suspend fun dimensions(date: Date) =
            values.associate { (key, value) -> key to RulesDimensionValue.StringValue(value) }
    }
}
