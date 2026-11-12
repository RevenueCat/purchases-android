@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensions
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsReceiptStore
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsResolution
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.utils.getNullableString
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AudienceCompatibilityTest {

    @Test
    fun `Khepri conditions match SDK results`() = runTest {
        val profiles = document("profiles.json").getJSONObject("profiles")
        val evaluators = profiles.keys().asSequence().associateWith { name ->
            evaluator(profiles.getJSONObject(name))
        }
        val conditionFiles = document("coverage.json").getJSONArray("condition_files")
        assertThat(conditionFiles.length()).isGreaterThan(0)
        val assertions = SoftAssertions()
        for (fileIndex in 0 until conditionFiles.length()) {
            val file = conditionFiles.getString(fileIndex)
            val cases = document(file).getJSONArray("cases")
            assertThat(cases.length()).describedAs(file).isGreaterThan(0)
            for (caseIndex in 0 until cases.length()) {
                val case = cases.getJSONObject(caseIndex)
                val id = case.getString("id")
                val evaluator = requireNotNull(evaluators[case.getString("profile")]) { id }
                val rule = object : LocalRule {
                    override val predicate = case.getString("predicate")
                }
                val result = evaluator.match(listOf(rule))
                if (result.isFailure) {
                    assertions.assertThat(result.exceptionOrNull()).describedAs(id).isNull()
                } else {
                    assertions.assertThat(result.getOrNull() != null)
                        .describedAs(id).isEqualTo(case.getBoolean("expected"))
                }
            }
        }
        assertions.assertAll()
    }

    @Test
    fun `every condition has executable coverage`() {
        val coverage = document("coverage.json").getJSONArray("coverage")
        assertThat(coverage.length()).isGreaterThan(0)
        val incomplete = (0 until coverage.length()).map { coverage.getJSONObject(it) }
            .filter { it.getString("status") != "covered" }
            .map { "${it.getString("field")}/${it.getString("operator")}: ${it.getString("status")}" }
        assertThat(incomplete).describedAs("Conditions without executable coverage").isEmpty()
    }

    private fun document(name: String): JSONObject {
        val stream = requireNotNull(javaClass.getResourceAsStream("/audience_compatibility/$name")) {
            "Missing Khepri test data: $name"
        }
        val document = JSONObject(stream.bufferedReader().use { it.readText() })
        assertThat(document.getInt("schema_version")).describedAs(name).isEqualTo(1)
        return document
    }

    private fun evaluator(profile: JSONObject): LocalRulesEvaluator {
        val user = profile.getString("app_user_id")
        val device = profile.getJSONObject("device")
        val locale = device.getNullableString("locale").orEmpty()
        val appConfig = mockk<AppConfig> {
            every { versionName } returns device.getString("app_version")
            every { languageTag } returns locale
            every { store } returns Store.PLAY_STORE
        }
        val localeProvider = object : LocaleProvider {
            override val currentLocalesLanguageTags = locale
        }
        val customerInfo = CustomerInfoFactory.buildCustomerInfo(
            profile.getJSONObject("customer_info"), null, VerificationResult.NOT_REQUESTED,
        )
        val attributes = profile.getJSONObject("attributes").let { values ->
            values.keys().asSequence().associateWith { key ->
                val attribute = values.getJSONObject(key)
                SubscriberAttribute(
                    key = key,
                    value = attribute.getNullableString("value"),
                    setTime = Date(attribute.getLong("updated_at") * 1_000),
                    isSynced = true,
                )
            }
        }
        val dimensions = requireNotNull(SubscriberDimensions.parse(
            Json.parseToJsonElement(profile.getJSONObject("subscriber_dimensions").toString()).jsonObject,
        ))
        val cache = mockk<DeviceCache> {
            every { getCachedSubscriberDimensionsJson(user) } returns null
        }
        val storeCountry = androidStoreCountry(profile.getNullableString("store_country"))
        return LocalRulesEvaluator(
            providers = listOf(
                DeviceDimensionProvider(appConfig, localeProvider),
                StoreDimensionProvider { storeCountry },
                CustomerInfoDimensionProvider({ user }) { customerInfo },
                SubscriberAttributesDimensionProvider { attributes },
                SubscriberDimensionsProvider(
                    configDimensions = { SubscriberDimensionsResolution.Found(dimensions) },
                    receiptStore = SubscriberDimensionsReceiptStore(cache),
                    currentAppUserId = { user },
                ),
            ),
            currentAppUserId = { user },
            dateProvider = object : DateProvider {
                override val now = Date(profile.getLong("evaluated_at") * 1_000)
            },
        )
    }

    private fun androidStoreCountry(alpha3: String?): String? {
        if (alpha3 == null) return null
        return requireNotNull(Locale.getISOCountries().firstOrNull { country ->
            Locale.Builder().setRegion(country).build().isO3Country == alpha3
        }) { "Cannot represent store country $alpha3 in Android test inputs" }
    }
}
