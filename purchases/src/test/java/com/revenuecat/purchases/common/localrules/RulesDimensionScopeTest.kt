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
              "custom": {
                "plan": "gold",
                "seats": 3,
                "trialEligible": true
              },
              "customerInfo": {
                "appUserId": "current_user",
                "entitlements": [
                  {
                    "billingIssueDetectedAt": 1714780800000,
                    "evaluatedAt": 1718452800000,
                    "expiresAt": 4102444800000,
                    "identifier": "extra",
                    "isActive": true,
                    "isSandbox": true,
                    "latestPurchasedAt": 1714521600000,
                    "originalPurchasedAt": 1609459200000,
                    "ownershipType": "PURCHASED",
                    "periodType": "trial",
                    "productIdentifier": "premium",
                    "productPlanIdentifier": "monthly",
                    "purchasedProductIdentifier": "premium:monthly",
                    "store": "play_store",
                    "unsubscribeDetectedAt": 1714694400000,
                    "willRenew": false
                  },
                  {
                    "billingIssueDetectedAt": 1714780800000,
                    "evaluatedAt": 1718452800000,
                    "expiresAt": 4102444800000,
                    "identifier": "premium",
                    "isActive": true,
                    "isSandbox": true,
                    "latestPurchasedAt": 1714521600000,
                    "originalPurchasedAt": 1609459200000,
                    "ownershipType": "PURCHASED",
                    "periodType": "trial",
                    "productIdentifier": "premium",
                    "productPlanIdentifier": "monthly",
                    "purchasedProductIdentifier": "premium:monthly",
                    "store": "play_store",
                    "unsubscribeDetectedAt": 1714694400000,
                    "willRenew": false
                  }
                ],
                "evaluatedAt": 1718452800000,
                "firstSeenAt": 1640995200000,
                "lastSeenAt": 1717200000000,
                "originalAppUserId": "original_user",
                "originalPurchasedAt": 1609459200000,
                "purchases": [
                  {
                    "autoResumeAt": 1717200000000,
                    "billingIssueDetectedAt": 1714780800000,
                    "displayName": "Premium Monthly",
                    "evaluatedAt": 1718452800000,
                    "expiresAt": 4102444800000,
                    "gracePeriodExpiresAt": 1715299200000,
                    "isActive": true,
                    "isInGracePeriod": false,
                    "isPaused": true,
                    "isRefunded": true,
                    "isSandbox": true,
                    "kind": "subscription",
                    "originalPurchasedAt": 1609459200000,
                    "ownershipType": "PURCHASED",
                    "periodType": "trial",
                    "priceAmountMicros": 4990000,
                    "priceCurrency": "USD",
                    "productIdentifier": "premium",
                    "productPlanIdentifier": "monthly",
                    "purchasedAt": 1714521600000,
                    "purchasedProductIdentifier": "premium:monthly",
                    "refundedAt": 1714867200000,
                    "status": "paused",
                    "store": "play_store",
                    "storeTransactionId": "GPA.0000-0000-0000-00000",
                    "unsubscribeDetectedAt": 1714694400000,
                    "willRenew": false
                  },
                  {
                    "displayName": "100 Coins",
                    "evaluatedAt": 1718452800000,
                    "isSandbox": false,
                    "kind": "nonSubscription",
                    "originalPurchasedAt": 1677801600000,
                    "priceAmountMicros": 1990000,
                    "priceCurrency": "EUR",
                    "productIdentifier": "coins",
                    "purchasedAt": 1677801600000,
                    "purchasedProductIdentifier": "coins",
                    "store": "amazon",
                    "storeTransactionId": "amzn.1234",
                    "transactionIdentifier": "abc123"
                  }
                ]
              },
              "device": {
                "appVersion": "1.2.3",
                "locale": "en_us",
                "platform": "android",
                "platformVersion": 34,
                "sdkVersion": "${Config.frameworkVersion}"
              },
              "store": {
                "country": "USA"
              },
              "subscriberAttributes": {
                "${'$'}email": {
                  "evaluatedAt": 1718452800000,
                  "updatedAt": 1714521600000,
                  "value": "jane@example.com"
                },
                "goal": {
                  "evaluatedAt": 1718452800000,
                  "updatedAt": 1718366400000,
                  "value": "lose_weight"
                },
                "seats": {
                  "evaluatedAt": 1718452800000,
                  "updatedAt": 1714780800000,
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
            """{"==": [{"var": "device.platform"}, "android"]}""",
            """{"==": [{"var": "store.country"}, "USA"]}""",
            """{"==": [{"var": "custom.plan"}, "gold"]}""",
            """{"==": [{"var": "customerInfo.appUserId"}, "current_user"]}""",
            """{"some": [{"var": "customerInfo.purchases"}, {"==": [{"var": "periodType"}, "trial"]}]}""",
            """{"some": [{"var": "customerInfo.entitlements"}, {"var": "isActive"}]}""",
            """{"==": [{"var": "subscriberAttributes.${'$'}email.value"}, "jane@example.com"]}""",
            """{"<": [{"-": [{"var": "subscriberAttributes.goal.evaluatedAt"},
                {"var": "subscriberAttributes.goal.updatedAt"}]}, 604800000]}""",
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
                    appUserId = { "current_user" },
                    customerInfo = { customerInfo },
                ),
                SubscriberAttributesDimensionProvider { SUBSCRIBER_ATTRIBUTES },
            ),
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
