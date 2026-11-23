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
import com.revenuecat.purchases.utils.DateHelper
import com.revenuecat.purchases.utils.getNullableString
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.json.JSONObject
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date
import java.util.Locale

@RunWith(Enclosed::class)
class AudienceCompatibilityTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(manifest = Config.NONE)
    class Conditions(
        private val id: String,
        private val caseJson: String,
    ) {
        @Test
        fun `Khepri condition matches SDK result`() = runTest {
            val case = JSONObject(caseJson)
            val profiles = document("profiles.json").getValue("profiles").jsonObject
            val profile = profiles.getValue(case.getString("profile"))
            val profileJson = JSONObject(profile.toString())
            val clock = object : DateProvider {
                override val now = Date(profileJson.getLong("evaluated_at") * 1_000)
            }
            mockkObject(DateHelper.Companion) {
                every { DateHelper.now } returns clock.now
                val evaluator = evaluator(profileJson, clock)
                val rule = object : LocalRule {
                    override val predicate = case.getString("predicate")
                }
                val result = evaluator.match(listOf(rule))
                assertThat(result.exceptionOrNull()).describedAs(id).isNull()
                assertThat(result.getOrNull() != null).describedAs(id).isEqualTo(case.getBoolean("expected"))
            }
        }

        companion object {
            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
            fun parameters(): Collection<Array<String>> = cases().map { case ->
                arrayOf(case.getValue("id").jsonPrimitive.content, case.toString())
            }
        }
    }

    @RunWith(AndroidJUnit4::class)
    @Config(manifest = Config.NONE)
    class Coverage {
        @Test
        fun `required conditions have executable coverage`() {
            val report = document("coverage.json")
            val requiredFields = report.getValue("required_fields").jsonArray.map { it.jsonPrimitive.content }.toSet()
            assertThat(requiredFields).isNotEmpty()
            val required = report.getValue("coverage").jsonArray.map { it.jsonObject }
                .filter { it.getValue("field").jsonPrimitive.content in requiredFields }
            assertThat(required.map { it.getValue("field").jsonPrimitive.content }.toSet()).isEqualTo(requiredFields)
            val outcomes = cases().groupBy {
                it.getValue("field").jsonPrimitive.content to it.getValue("operator").jsonPrimitive.content
            }.mapValues { (_, cases) -> cases.map { it.getValue("expected").jsonPrimitive.boolean }.toSet() }
            val assertions = SoftAssertions()
            for (condition in required) {
                val field = condition.getValue("field").jsonPrimitive.content
                val operator = condition.getValue("operator").jsonPrimitive.content
                assertions.assertThat(condition.getValue("status").jsonPrimitive.content)
                    .describedAs("$field/$operator coverage status").isEqualTo("covered")
                assertions.assertThat(outcomes[field to operator].orEmpty())
                    .describedAs("$field/$operator expected outcomes").containsExactlyInAnyOrder(true, false)
            }
            assertions.assertAll()
        }
    }

    private companion object {
        fun document(name: String): JsonObject {
            val stream = requireNotNull(AudienceCompatibilityTest::class.java.getResourceAsStream(
                "/audience_compatibility/$name",
            )) { "Missing Khepri test data: $name" }
            val document = Json.parseToJsonElement(stream.bufferedReader().use { it.readText() }).jsonObject
            assertThat(document.getValue("schema_version").jsonPrimitive.int).describedAs(name).isEqualTo(1)
            return document
        }

        fun cases(): List<JsonObject> {
            val files = document("coverage.json").getValue("condition_files").jsonArray
            assertThat(files).isNotEmpty()
            return files.flatMap { file ->
                val name = file.jsonPrimitive.content
                val cases = document(name).getValue("cases").jsonArray
                assertThat(cases).describedAs(name).isNotEmpty()
                cases.map { it.jsonObject }
            }
        }

        private fun evaluator(profile: JSONObject, clock: DateProvider): LocalRulesEvaluator {
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
                providers = localRulesDimensionProviders(
                    appConfig = appConfig,
                    localeProvider = localeProvider,
                    storefront = { storeCountry },
                    attributes = { attributes },
                    customerInfo = { customerInfo },
                    dimensions = { SubscriberDimensionsResolution.Found(dimensions) },
                    receiptStore = SubscriberDimensionsReceiptStore(cache),
                    currentAppUserId = { user },
                ),
                currentAppUserId = { user },
                dateProvider = clock,
            )
        }

        private fun androidStoreCountry(alpha3: String?): String? {
            if (alpha3 == null) return null
            return requireNotNull(Locale.getISOCountries().firstOrNull { country ->
                Locale.Builder().setRegion(country).build().isO3Country == alpha3
            }) { "Cannot represent store country $alpha3 in Android test inputs" }
        }
    }
}
