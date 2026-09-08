@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.utils.Iso8601Utils
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import org.robolectric.annotation.Config as RobolectricConfig

/**
 * The complete scope a predicate is evaluated against, spelled out.
 *
 * Every other test in this package covers one source in isolation. This one wires all of them together the way
 * [com.revenuecat.purchases.PurchasesFactory] does and writes down the whole result, so the answer to "what can a
 * rule actually read?" is one file rather than a survey of every provider — for whoever authors a predicate, and
 * for the platform that has to expose the same scope under the same names.
 *
 * The customer is deliberately not a realistic one: they are refunded *and* renewing, in a trial *and* paused, so
 * that every key appears once. A customer who really is any of those things has the absent keys left out.
 *
 * Dates are epoch milliseconds, which is the only form the engine has. The ISO instants they were built from are
 * in [SUBSCRIBER_RESPONSE] below; the evaluation instant is 2024-06-15T12:00:00Z.
 *
 * When this fails after an intended change, the assertion message contains the new scope verbatim.
 *
 * It does not verify that [com.revenuecat.purchases.PurchasesFactory] wires exactly these providers, since that
 * would mean configuring an SDK instance: a provider added there and not here goes unnoticed by this test.
 */
@RunWith(AndroidJUnit4::class)
@RobolectricConfig(manifest = RobolectricConfig.NONE, sdk = [34])
class RulesDimensionScopeTest {

    private val evaluationDate = Iso8601Utils.parse("2024-06-15T12:00:00Z")

    @Test
    fun `the whole evaluation scope`() = runTest {
        val scope = resolver().snapshot(CUSTOM_VARIABLES).getOrThrow().values

        assertThat(scope.render()).isEqualTo(
            """
            {
              "acquisition_channel": "paid_search",
              "app_user_id": "current_user",
              "app_version": "1.2.3",
              "custom": {
                "plan": "gold",
                "seats": 3,
                "trialEligible": true
              },
              "entitlements": [
                {
                  "billing_issue_detected_at": 1714780800000,
                  "expires_at": 4102444800000,
                  "identifier": "extra",
                  "is_active": true,
                  "is_sandbox": true,
                  "latest_purchased_at": 1714521600000,
                  "original_purchased_at": 1609459200000,
                  "ownership_type": "PURCHASED",
                  "period_type": "trial",
                  "product_identifier": "premium",
                  "product_plan_identifier": "monthly",
                  "purchased_product_identifier": "premium:monthly",
                  "store": "play_store",
                  "unsubscribe_detected_at": 1714694400000,
                  "will_renew": false
                },
                {
                  "billing_issue_detected_at": 1714780800000,
                  "expires_at": 4102444800000,
                  "identifier": "premium",
                  "is_active": true,
                  "is_sandbox": true,
                  "latest_purchased_at": 1714521600000,
                  "original_purchased_at": 1609459200000,
                  "ownership_type": "PURCHASED",
                  "period_type": "trial",
                  "product_identifier": "premium",
                  "product_plan_identifier": "monthly",
                  "purchased_product_identifier": "premium:monthly",
                  "store": "play_store",
                  "unsubscribe_detected_at": 1714694400000,
                  "will_renew": false
                }
              ],
              "evaluated_at": 1718452800000,
              "first_seen_at": 1640995200000,
              "locale": "en_us",
              "original_app_user_id": "original_user",
              "original_purchased_at": 1609459200000,
              "platform": "android",
              "platform_version": "34",
              "predicted_ltv_band": 3,
              "purchases": [
                {
                  "auto_resume_at": 1717200000000,
                  "billing_issue_detected_at": 1714780800000,
                  "display_name": "Premium Monthly",
                  "expires_at": 4102444800000,
                  "grace_period_expires_at": 1715299200000,
                  "is_active": true,
                  "is_in_grace_period": false,
                  "is_paused": true,
                  "is_refunded": true,
                  "is_sandbox": true,
                  "kind": "subscription",
                  "original_purchased_at": 1609459200000,
                  "ownership_type": "PURCHASED",
                  "period_type": "trial",
                  "price_amount_micros": 4990000,
                  "price_currency": "USD",
                  "product_identifier": "premium",
                  "product_plan_identifier": "monthly",
                  "purchased_at": 1714521600000,
                  "purchased_product_identifier": "premium:monthly",
                  "refunded_at": 1714867200000,
                  "status": "paused",
                  "store": "play_store",
                  "store_transaction_id": "GPA.0000-0000-0000-00000",
                  "unsubscribe_detected_at": 1714694400000,
                  "will_renew": false
                },
                {
                  "display_name": "100 Coins",
                  "is_sandbox": false,
                  "kind": "non_subscription",
                  "original_purchased_at": 1677801600000,
                  "price_amount_micros": 1990000,
                  "price_currency": "EUR",
                  "product_identifier": "coins",
                  "purchased_at": 1677801600000,
                  "purchased_product_identifier": "coins",
                  "store": "amazon",
                  "store_transaction_id": "amzn.1234",
                  "transaction_identifier": "abc123"
                }
              ],
              "sdk_version": "${Config.frameworkVersion}",
              "storefront": "USA",
              "subscriber_attributes": {
                "${'$'}email": {
                  "updated_at": 1714521600000,
                  "value": "jane@example.com"
                },
                "goal": {
                  "updated_at": 1718366400000,
                  "value": "lose_weight"
                },
                "seats": {
                  "updated_at": 1714780800000,
                  "value": "3"
                }
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `every root of the scope is readable by a predicate`() = runTest {
        val scope = resolver().snapshot(CUSTOM_VARIABLES).getOrThrow().values

        val predicates = listOf(
            """{"==": [{"var": "platform"}, "android"]}""",
            """{"==": [{"var": "storefront"}, "USA"]}""",
            """{"==": [{"var": "custom.plan"}, "gold"]}""",
            """{"==": [{"var": "app_user_id"}, "current_user"]}""",
            """{"==": [{"var": "acquisition_channel"}, "paid_search"]}""",
            """{"some": [{"var": "purchases"}, {"==": [{"var": "period_type"}, "trial"]}]}""",
            """{"some": [{"var": "entitlements"}, {"var": "is_active"}]}""",
            """{"==": [{"var": "subscriber_attributes.${'$'}email.value"}, "jane@example.com"]}""",
            """{"<": [{"-": [{"var": "evaluated_at"},
                {"var": "subscriber_attributes.goal.updated_at"}]}, 604800000]}""",
            // The root instant is not in scope inside an iteration operator, so a purchase compared against it is
            // read by index.
            """{">": [{"var": "purchases.0.expires_at"}, {"var": "evaluated_at"}]}""",
        )

        for (predicate in predicates) {
            assertThat(RulesEngine.evaluate(predicate, scope).getOrThrow()).describedAs(predicate).isTrue()
        }
    }

    private fun resolver(): RulesDimensionResolver {
        val appConfig = mockk<AppConfig>().also {
            every { it.store } returns Store.PLAY_STORE
            every { it.languageTag } returns "en-US"
            every { it.versionName } returns "1.2.3"
        }
        val localeProvider = object : LocaleProvider {
            override val currentLocalesLanguageTags: String get() = "en-US"
        }
        // Verified on purpose, and deliberately absent from the scope below: entitlement verification is not
        // something a rule is evaluated against.
        val customerInfo = CustomerInfoFactory.buildCustomerInfo(
            JSONObject(SUBSCRIBER_RESPONSE),
            null,
            VerificationResult.VERIFIED,
        )
        return RulesDimensionResolver(
            providers = listOf(
                DeviceDimensionProvider(appConfig, localeProvider),
                StoreDimensionProvider { "US" },
                CustomerInfoDimensionProvider(
                    currentAppUserId = { APP_USER_ID },
                    customerInfo = { customerInfo },
                ),
                SubscriberAttributesDimensionProvider { SUBSCRIBER_ATTRIBUTES },
                SubscriberDimensionsProvider { SUBSCRIBER_DIMENSIONS },
            ),
            currentAppUserId = { APP_USER_ID },
            dateProvider = object : DateProvider {
                override val now: Date get() = evaluationDate
            },
        )
    }

    /**
     * Renders the scope the way the engine holds it, with object keys sorted so the shape is readable and stable.
     * Array order is left alone: it is part of the contract, since `purchases.0` is the most recent purchase.
     */
    private fun Map<String, Value>.render(): String = Value.ObjectValue(this).render(indent = "")

    private fun Value.render(indent: String): String = when (this) {
        is Value.ObjectValue -> entries.entries
            .sortedBy { (key, _) -> key }
            .joinToString(separator = ",\n", prefix = "{\n", postfix = "\n$indent}") { (key, value) ->
                """$indent  "$key": ${value.render("$indent  ")}"""
            }
            .takeIf { entries.isNotEmpty() } ?: "{}"
        is Value.ArrayValue -> items
            .joinToString(separator = ",\n", prefix = "[\n", postfix = "\n$indent]") { item ->
                "$indent  ${item.render("$indent  ")}"
            }
            .takeIf { items.isNotEmpty() } ?: "[]"
        is Value.StringValue -> "\"$value\""
        is Value.BoolValue -> value.toString()
        is Value.IntValue -> value.toString()
        is Value.FloatValue -> value.toString()
        Value.Null -> "null"
        Value.Undefined -> "undefined"
    }

    private companion object {

        const val APP_USER_ID = "current_user"

        /**
         * A reserved name, custom ones, a value that looks like a number but stays the string it was set as, and
         * two the scope leaves out: a deleted attribute, and a name a dot-path could not reach.
         */
        val SUBSCRIBER_ATTRIBUTES = listOf(
            subscriberAttribute("\$email", "jane@example.com", setAt = "2024-05-01T00:00:00Z"),
            subscriberAttribute("goal", "lose_weight", setAt = "2024-06-14T12:00:00Z"),
            subscriberAttribute("seats", "3", setAt = "2024-05-04T00:00:00Z"),
            subscriberAttribute("tier", null, setAt = "2024-05-04T00:00:00Z"),
            subscriberAttribute("user.tier", "gold", setAt = "2024-05-04T00:00:00Z"),
        ).associateBy { attribute -> attribute.key.backendKey }

        private fun subscriberAttribute(key: String, value: String?, setAt: String) = SubscriberAttribute(
            key = key,
            value = value,
            setTime = Iso8601Utils.parse(setAt),
            isSynced = true,
        )

        val CUSTOM_VARIABLES = mapOf(
            "plan" to RulesDimensionValue.StringValue("gold"),
            "seats" to RulesDimensionValue.IntValue(3),
            "trialEligible" to RulesDimensionValue.BoolValue(true),
        )

        /** The dimensions the backend last sent alongside the subscriber, root-level under their own names. */
        val SUBSCRIBER_DIMENSIONS = """
            {
              "acquisition_channel": "paid_search",
              "predicted_ltv_band": 3
            }
        """

        /**
         * One Google subscription with every field the backend can send, one Amazon one-time purchase, and two
         * entitlements unlocked by the subscription.
         */
        val SUBSCRIBER_RESPONSE = """
            {
              "request_date": "2024-06-01T00:00:00Z",
              "subscriber": {
                "original_app_user_id": "original_user",
                "first_seen": "2022-01-01T00:00:00Z",
                "original_purchase_date": "2021-01-01T00:00:00Z",
                "management_url": "https://play.google.com/store/account/subscriptions",
                "subscriptions": {
                  "premium": {
                    "store": "play_store",
                    "product_plan_identifier": "monthly",
                    "purchase_date": "2024-05-01T00:00:00Z",
                    "original_purchase_date": "2021-01-01T00:00:00Z",
                    "expires_date": "2100-01-01T00:00:00Z",
                    "period_type": "trial",
                    "ownership_type": "PURCHASED",
                    "is_sandbox": true,
                    "unsubscribe_detected_at": "2024-05-03T00:00:00Z",
                    "billing_issues_detected_at": "2024-05-04T00:00:00Z",
                    "grace_period_expires_date": "2024-05-10T00:00:00Z",
                    "refunded_at": "2024-05-05T00:00:00Z",
                    "auto_resume_date": "2024-06-01T00:00:00Z",
                    "display_name": "Premium Monthly",
                    "price": { "amount": 4.99, "currency": "USD" },
                    "store_transaction_id": "GPA.0000-0000-0000-00000"
                  }
                },
                "non_subscriptions": {
                  "coins": [
                    {
                      "id": "abc123",
                      "is_sandbox": false,
                      "original_purchase_date": "2023-03-03T00:00:00Z",
                      "purchase_date": "2023-03-03T00:00:00Z",
                      "store": "amazon",
                      "display_name": "100 Coins",
                      "price": { "amount": 1.99, "currency": "EUR" },
                      "store_transaction_id": "amzn.1234"
                    }
                  ]
                },
                "entitlements": {
                  "premium": {
                    "expires_date": "2100-01-01T00:00:00Z",
                    "product_identifier": "premium",
                    "product_plan_identifier": "monthly",
                    "purchase_date": "2024-05-01T00:00:00Z"
                  },
                  "extra": {
                    "expires_date": "2100-01-01T00:00:00Z",
                    "product_identifier": "premium",
                    "product_plan_identifier": "monthly",
                    "purchase_date": "2024-05-01T00:00:00Z"
                  }
                }
              }
            }
        """
    }
}
