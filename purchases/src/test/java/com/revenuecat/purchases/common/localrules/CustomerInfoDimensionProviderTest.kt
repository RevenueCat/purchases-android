@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.Responses
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomerInfoDimensionProviderTest {

    private val date = Date(1_700_000_000_000)

    @Test
    fun `provides the customer's identity and lifecycle dimensions`() = runTest {
        val dimensions = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date)

        assertThat(dimensions.filterValues { it !is RulesDimensionValue.ObjectListValue }).isEqualTo(
            mapOf(
                "app_user_id" to string(APP_USER_ID),
                "original_app_user_id" to string("original_user"),
                "first_seen_at" to date("2022-01-01T00:00:00Z"),
                "original_purchased_at" to date("2021-01-01T00:00:00Z"),
            ),
        )
    }

    @Test
    fun `describes every purchase of either kind, newest first`() = runTest {
        val purchases = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date).purchases()

        assertThat(purchases.map { it["kind"] to it["purchased_product_identifier"] }).containsExactly(
            string("subscription") to string("premium:monthly"),
            string("non_subscription") to string("coins"),
            string("subscription") to string("legacy:annual"),
        )
    }

    @Test
    fun `describes a subscription with everything the SDK knows about it`() = runTest {
        val purchases = provider(customerInfo(FULLY_POPULATED_RESPONSE)).dimensions(date).purchases()

        assertThat(purchases.single()).isEqualTo(
            mapOf(
                "kind" to string("subscription"),
                "product_identifier" to string("premium"),
                "product_plan_identifier" to string("monthly"),
                "purchased_product_identifier" to string("premium:monthly"),
                "store_transaction_id" to string("GPA.0000-0000-0000-00000"),
                "display_name" to string("Premium Monthly"),
                "store" to string("play_store"),
                "ownership_type" to string("PURCHASED"),
                "period_type" to string("trial"),
                "status" to string("paused"),
                "price_amount_micros" to RulesDimensionValue.IntValue(4_990_000),
                "price_currency" to string("USD"),
                "purchased_at" to date("2024-05-01T00:00:00Z"),
                "original_purchased_at" to date("2021-01-01T00:00:00Z"),
                "expires_at" to date("2100-01-01T00:00:00Z"),
                "unsubscribe_detected_at" to date("2024-05-03T00:00:00Z"),
                "billing_issue_detected_at" to date("2024-05-04T00:00:00Z"),
                "grace_period_expires_at" to date("2024-05-10T00:00:00Z"),
                "refunded_at" to date("2024-05-05T00:00:00Z"),
                "auto_resume_at" to date("2024-06-01T00:00:00Z"),
                "is_sandbox" to bool(true),
                "is_active" to bool(true),
                "will_renew" to bool(false),
                "is_in_grace_period" to bool(true),
                "is_refunded" to bool(true),
                "is_paused" to bool(true),
            ),
        )
    }

    @Test
    fun `describes a one-time purchase without the fields only a subscription has`() = runTest {
        val purchase = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date).purchases()
            .single { it["kind"] == string("non_subscription") }

        assertThat(purchase).isEqualTo(
            mapOf(
                "kind" to string("non_subscription"),
                "product_identifier" to string("coins"),
                "purchased_product_identifier" to string("coins"),
                "transaction_identifier" to string("abc123"),
                "store" to string("play_store"),
                "purchased_at" to date("2023-03-03T00:00:00Z"),
                "original_purchased_at" to date("2023-03-03T00:00:00Z"),
                "is_sandbox" to bool(true),
            ),
        )
    }

    @Test
    fun `describes every entitlement, ordered by identifier`() = runTest {
        val entitlements = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date).entitlements()

        assertThat(entitlements.map { it["identifier"] })
            .containsExactly(string("extra"), string("old"), string("premium"))
        assertThat(entitlements.single { it["identifier"] == string("old") }).isEqualTo(
            mapOf(
                "identifier" to string("old"),
                "product_identifier" to string("legacy"),
                "product_plan_identifier" to string("annual"),
                "purchased_product_identifier" to string("legacy:annual"),
                "store" to string("amazon"),
                "ownership_type" to string("FAMILY_SHARED"),
                "period_type" to string("normal"),
                "latest_purchased_at" to date("2022-06-01T00:00:00Z"),
                "original_purchased_at" to date("2022-06-01T00:00:00Z"),
                "expires_at" to date("2023-06-01T00:00:00Z"),
                "is_sandbox" to bool(true),
                "is_active" to bool(false),
                "will_renew" to bool(true),
            ),
        )
    }

    @Test
    fun `reports where the customer is in each subscription's lifecycle`() = runTest {
        val cases = listOf(
            Triple("a paid subscription", "active", statusResponse(expiresDate = FUTURE)),
            Triple("a free trial", "trialing", statusResponse(expiresDate = FUTURE, periodType = "trial")),
            // Still being served while the billing issue is retried, which outranks the trial it interrupted.
            Triple(
                "a trial in a grace period",
                "in_grace_period",
                statusResponse(
                    expiresDate = FUTURE,
                    periodType = "trial",
                    billingIssuesDetectedAt = PAST,
                    gracePeriodExpiresDate = FUTURE,
                ),
            ),
            // Paused whatever the dates say.
            Triple("a paused subscription", "paused", statusResponse(expiresDate = FUTURE, autoResumeDate = FUTURE)),
            Triple("a lapsed subscription", "expired", statusResponse(expiresDate = PAST)),
            // The device cannot tell a store still retrying from one that gave up long ago, so this does not claim
            // `in_billing_retry`; `billing_issue_detected_at` is on the record for a predicate that needs it.
            Triple(
                "a subscription that lapsed after a billing issue",
                "expired",
                statusResponse(expiresDate = PAST, billingIssuesDetectedAt = PAST),
            ),
        )

        for ((label, expected, response) in cases) {
            val purchase = provider(customerInfo(response)).dimensions(date).purchases().single()

            assertThat(purchase["status"]).describedAs(label).isEqualTo(string(expected))
        }
    }

    @Test
    fun `a lifetime subscription is active`() = runTest {
        val purchase = provider(customerInfo(LIFETIME_RESPONSE)).dimensions(date).purchases().single()

        assertThat(purchase["status"]).isEqualTo(string("active"))
    }

    @Test
    fun `the grace period status and the grace period flag always agree`() = runTest {
        val purchase = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date).purchases()
            .single { it["purchased_product_identifier"] == string("legacy:annual") }

        assertThat(purchase["is_in_grace_period"]).isEqualTo(bool(true))
        assertThat(purchase["status"]).isEqualTo(string("in_grace_period"))
    }

    @Test
    fun `a one-time purchase has no status, since only a subscription has a lifecycle`() = runTest {
        val purchase = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date).purchases()
            .single { it["kind"] == string("non_subscription") }

        assertThat(purchase).doesNotContainKey("status")
    }

    @Test
    fun `a customer who has never bought anything reports empty collections rather than none`() = runTest {
        val dimensions = provider(customerInfo(Responses.validEmptyPurchaserResponse)).dimensions(date)

        // An empty array is what makes `none` a definite yes and `some` a definite no; an absent key would leave
        // both unknown.
        assertThat(dimensions["purchases"]).isEqualTo(RulesDimensionValue.ObjectListValue(emptyList()))
        assertThat(dimensions["entitlements"]).isEqualTo(RulesDimensionValue.ObjectListValue(emptyList()))
    }

    @Test
    fun `a lifetime purchase reports no expiry and stays active`() = runTest {
        val purchase = provider(customerInfo(LIFETIME_RESPONSE)).dimensions(date).purchases().single()

        assertThat(purchase).doesNotContainKey("expires_at")
        assertThat(purchase["is_active"]).isEqualTo(bool(true))
        // Nothing to renew.
        assertThat(purchase["will_renew"]).isEqualTo(bool(false))
    }

    @Test
    fun `an unknown ownership type is reported as no ownership type at all`() = runTest {
        val purchase = provider(customerInfo(PROMO_RESPONSE)).dimensions(date).purchases().single()

        assertThat(purchase).doesNotContainKey("ownership_type")
        assertThat(purchase["store"]).isEqualTo(string("promotional"))
    }

    @Test
    fun `a subscription being served through a grace period says so`() = runTest {
        val purchase = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date).purchases()
            .single { it["purchased_product_identifier"] == string("legacy:annual") }

        // Expired in 2023, but the store keeps serving it until 2100.
        assertThat(purchase["is_active"]).isEqualTo(bool(false))
        assertThat(purchase["is_in_grace_period"]).isEqualTo(bool(true))
    }

    @Test
    fun `a customer info that cannot be read leaves the identity dimensions usable`() = runTest {
        // Not just PurchasesException: the read goes through the configured instance, which an app can tear down
        // mid-evaluation, and it can fall back to the network.
        val failures = listOf(
            PurchasesException(PurchasesError(PurchasesErrorCode.NetworkError, "Nope.")),
            UninitializedPropertyAccessException("There is no singleton instance."),
            IllegalStateException("Something else entirely."),
        )

        for (failure in failures) {
            val snapshot = resolver(provider { throw failure }).snapshot()

            assertThat(snapshot.isSuccess).describedAs("%s", failure).isTrue()
            // The app user ID is known without asking the backend, so a rule on it survives the failure.
            val values = snapshot.getOrThrow().values
            assertThat(values["app_user_id"]).describedAs("%s", failure)
                .isEqualTo(Value.StringValue(APP_USER_ID))
            assertThat(values).describedAs("%s", failure).doesNotContainKey("purchases")
        }
    }

    @Test
    fun `the customer info is requested for the app user the dimensions report`() = runTest {
        var requestedAppUserId: String? = null
        val provider = provider { appUserId ->
            requestedAppUserId = appUserId
            customerInfo(SUBSCRIBED_RESPONSE)
        }

        val dimensions = provider.dimensions(date)

        // Both come from the same read of the current ID, so they cannot describe two different customers.
        assertThat(requestedAppUserId).isEqualTo(APP_USER_ID)
        assertThat(dimensions["app_user_id"]).isEqualTo(string(APP_USER_ID))
        assertThat(dimensions.purchases()).isNotEmpty()
    }

    @Test
    fun `a customer the SDK has no ID for is not asked about`() = runTest {
        var asked = false
        val provider = CustomerInfoDimensionProvider(
            currentAppUserId = { "" },
            customerInfo = {
                asked = true
                customerInfo(SUBSCRIBED_RESPONSE)
            },
        )

        val dimensions = provider.dimensions(date)

        assertThat(asked).isFalse()
        assertThat(dimensions).isEmpty()
    }

    @Test
    fun `cancellation while reading the customer info propagates`() = runTest {
        val cancelling = provider { throw CancellationException("cancelled") }

        val thrown = try {
            cancelling.dimensions(date)
            null
        } catch (e: CancellationException) {
            e
        }

        assertThat(thrown).hasMessage("cancelled")
    }

    @Test
    fun `the customer info is read on every evaluation`() = runTest {
        var response = Responses.validEmptyPurchaserResponse
        val provider = provider { customerInfo(response) }

        assertThat(provider.dimensions(date).purchases()).isEmpty()

        response = SUBSCRIBED_RESPONSE

        assertThat(provider.dimensions(date).purchases()).hasSize(3)
    }

    @Test
    fun `the records are searchable by a predicate`() = runTest {
        val values = resolver(provider(customerInfo(SUBSCRIBED_RESPONSE))).snapshot().getOrThrow().values

        val matching = listOf(
            """{"==": [{"var": "app_user_id"}, "$APP_USER_ID"]}""",
            """{"some": [{"var": "entitlements"},
                 {"and": [{"==": [{"var": "identifier"}, "premium"]}, {"var": "is_active"}]}]}""",
            """{"some": [{"var": "purchases"},
                 {"==": [{"var": "purchased_product_identifier"}, "legacy:annual"]}]}""",
            // The latest purchase of any kind, with no iteration at all.
            """{"==": [{"var": "purchases.0.product_identifier"}, "premium"]}""",
            // Ends more than a day from the evaluation instant. Read by index, since the root instant is not
            // in scope inside an iteration operator.
            """{">": [{"-": [{"var": "purchases.0.expires_at"}, {"var": "evaluated_at"}]}, 86400000]}""",
            // A one-time purchase omits `is_refunded`, and negation cannot match on an omitted variable, so a
            // predicate across both kinds spells out the default.
            """{"none": [{"var": "purchases"}, {"var": ["is_refunded", false]}]}""",
        )
        val notMatching = listOf(
            """{"some": [{"var": "entitlements"},
                 {"and": [{"==": [{"var": "identifier"}, "old"]}, {"var": "is_active"}]}]}""",
            """{"some": [{"var": "purchases"}, {"==": [{"var": ["period_type", ""]}, "trial"]}]}""",
            """{"some": [{"var": "purchases"},
                 {"==": [{"var": "purchased_product_identifier"}, "legacy"]}]}""",
        )

        for (predicate in matching) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isTrue()
        }
        for (predicate in notMatching) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isFalse()
        }
    }

    private fun provider(customerInfo: CustomerInfo) = provider { customerInfo }

    private fun provider(customerInfo: suspend (appUserId: String) -> CustomerInfo) = CustomerInfoDimensionProvider(
        currentAppUserId = { APP_USER_ID },
        customerInfo = customerInfo,
    )

    private fun resolver(customerInfoProvider: RulesDimensionProvider) = RulesDimensionResolver(
        providers = listOf(deviceProvider(), customerInfoProvider),
    )

    private fun deviceProvider() = object : RulesDimensionProvider {
        override val name = "device"
        override suspend fun dimensions(date: Date) = mapOf("platform" to string("android"))
    }

    private fun customerInfo(response: String): CustomerInfo =
        CustomerInfoFactory.buildCustomerInfo(JSONObject(response), null, VerificationResult.NOT_REQUESTED)

    private fun Map<String, RulesDimensionValue>.purchases() = objectList("purchases")
    private fun Map<String, RulesDimensionValue>.entitlements() = objectList("entitlements")

    private fun Map<String, RulesDimensionValue>.objectList(key: String) =
        (this[key] as RulesDimensionValue.ObjectListValue).value

    private fun string(value: String) = RulesDimensionValue.StringValue(value)
    private fun bool(value: Boolean) = RulesDimensionValue.BoolValue(value)
    private fun date(iso8601: String) = RulesDimensionValue.DateValue(Iso8601Utils.parse(iso8601))

    private companion object {

        const val APP_USER_ID = "current_user"

        /**
         * A Google subscription bought most recently, an Amazon one that expired but is in a grace period, and a
         * one-time purchase in between. Two entitlements are unlocked by the latest subscription and one by the
         * older one.
         */
        val SUBSCRIBED_RESPONSE = subscriberResponse(
            subscriptions = """
                "premium": {
                  ${subscription(
                store = "play_store",
                purchaseDate = "2024-01-15T10:00:00Z",
                originalPurchaseDate = "2021-01-01T00:00:00Z",
                expiresDate = "2100-01-01T00:00:00Z",
                ownershipType = "PURCHASED",
            )}
                },
                "legacy": {
                  ${subscription(
                store = "amazon",
                purchaseDate = "2022-06-01T00:00:00Z",
                originalPurchaseDate = "2022-06-01T00:00:00Z",
                expiresDate = "2023-06-01T00:00:00Z",
                ownershipType = "FAMILY_SHARED",
                productPlanIdentifier = "annual",
                gracePeriodExpiresDate = "2100-01-01T00:00:00Z",
            )}
                }
            """,
            nonSubscriptions = """
                "coins": [
                  {
                    "id": "abc123",
                    "is_sandbox": true,
                    "original_purchase_date": "2023-03-03T00:00:00Z",
                    "purchase_date": "2023-03-03T00:00:00Z",
                    "store": "play_store"
                  }
                ]
            """,
            entitlements = """
                "premium": {
                  "expires_date": "2100-01-01T00:00:00Z",
                  "product_identifier": "premium",
                  "product_plan_identifier": "monthly",
                  "purchase_date": "2024-01-15T10:00:00Z"
                },
                "extra": {
                  "expires_date": "2100-01-01T00:00:00Z",
                  "product_identifier": "premium",
                  "product_plan_identifier": "monthly",
                  "purchase_date": "2024-01-15T10:00:00Z"
                },
                "old": {
                  "expires_date": "2023-06-01T00:00:00Z",
                  "product_identifier": "legacy",
                  "product_plan_identifier": "annual",
                  "purchase_date": "2022-06-01T00:00:00Z"
                }
            """,
        )

        /** Every field the backend can send on a subscription, so a record can be asserted whole. */
        val FULLY_POPULATED_RESPONSE = subscriberResponse(
            subscriptions = """
                "premium": {
                  ${subscription(
                store = "play_store",
                purchaseDate = "2024-05-01T00:00:00Z",
                originalPurchaseDate = "2021-01-01T00:00:00Z",
                expiresDate = "2100-01-01T00:00:00Z",
                ownershipType = "PURCHASED",
                periodType = "trial",
                unsubscribeDetectedAt = "2024-05-03T00:00:00Z",
                billingIssuesDetectedAt = "2024-05-04T00:00:00Z",
                gracePeriodExpiresDate = "2024-05-10T00:00:00Z",
                refundedAt = "2024-05-05T00:00:00Z",
                autoResumeDate = "2024-06-01T00:00:00Z",
                displayName = "Premium Monthly",
                price = """{"amount": 4.99, "currency": "USD"}""",
            )}
                }
            """,
        )

        val LIFETIME_RESPONSE = subscriberResponse(
            subscriptions = """
                "lifetime": {
                  ${subscription(
                store = "play_store",
                purchaseDate = "2024-05-01T00:00:00Z",
                originalPurchaseDate = "2024-05-01T00:00:00Z",
                expiresDate = null,
                ownershipType = "PURCHASED",
            )}
                }
            """,
        )

        val PROMO_RESPONSE = subscriberResponse(
            subscriptions = """
                "rc_promo_pro_monthly": {
                  ${subscription(
                store = "promotional",
                purchaseDate = "2024-05-01T00:00:00Z",
                originalPurchaseDate = "2024-05-01T00:00:00Z",
                expiresDate = "2100-01-01T00:00:00Z",
                ownershipType = null,
            )}
                }
            """,
        )

        const val FUTURE = "2100-01-01T00:00:00Z"

        // Before the evaluation date, so a grace period ending here has already ended.
        const val PAST = "2023-01-01T00:00:00Z"

        /** One subscription, naming only the fields a status is derived from. */
        private fun statusResponse(
            expiresDate: String?,
            periodType: String = "normal",
            billingIssuesDetectedAt: String? = null,
            gracePeriodExpiresDate: String? = null,
            autoResumeDate: String? = null,
        ) = subscriberResponse(
            subscriptions = """
                "premium": {
                  ${subscription(
            store = "play_store",
            purchaseDate = "2024-05-01T00:00:00Z",
            originalPurchaseDate = "2024-05-01T00:00:00Z",
            expiresDate = expiresDate,
            ownershipType = "PURCHASED",
            periodType = periodType,
            billingIssuesDetectedAt = billingIssuesDetectedAt,
            gracePeriodExpiresDate = gracePeriodExpiresDate,
            autoResumeDate = autoResumeDate,
        )}
                }
            """,
        )

        @Suppress("LongParameterList")
        private fun subscription(
            store: String,
            purchaseDate: String,
            originalPurchaseDate: String,
            expiresDate: String?,
            ownershipType: String?,
            productPlanIdentifier: String? = "monthly",
            periodType: String = "normal",
            unsubscribeDetectedAt: String? = null,
            billingIssuesDetectedAt: String? = null,
            gracePeriodExpiresDate: String? = null,
            refundedAt: String? = null,
            autoResumeDate: String? = null,
            displayName: String? = null,
            price: String = "null",
        ) = """
            "store": "$store",
            "product_plan_identifier": ${productPlanIdentifier.asJsonString()},
            "purchase_date": "$purchaseDate",
            "original_purchase_date": "$originalPurchaseDate",
            "expires_date": ${expiresDate.asJsonString()},
            "period_type": "$periodType",
            "is_sandbox": true,
            "unsubscribe_detected_at": ${unsubscribeDetectedAt.asJsonString()},
            "billing_issues_detected_at": ${billingIssuesDetectedAt.asJsonString()},
            "grace_period_expires_date": ${gracePeriodExpiresDate.asJsonString()},
            "auto_resume_date": ${autoResumeDate.asJsonString()},
            "refunded_at": ${refundedAt.asJsonString()},
            "display_name": ${displayName.asJsonString()},
            "price": $price,
            "store_transaction_id": "GPA.0000-0000-0000-00000"${ownershipType.asOwnershipTypeEntry()}
        """

        private fun subscriberResponse(
            subscriptions: String = "",
            nonSubscriptions: String = "",
            entitlements: String = "",
        ) = """
            {
              "request_date": "2024-06-01T00:00:00Z",
              "subscriber": {
                "original_app_user_id": "original_user",
                "original_application_version": "1.0",
                "first_seen": "2022-01-01T00:00:00Z",
                "original_purchase_date": "2021-01-01T00:00:00Z",
                "management_url": null,
                "subscriptions": { $subscriptions },
                "non_subscriptions": { $nonSubscriptions },
                "entitlements": { $entitlements }
              }
            }
        """

        private fun String?.asJsonString() = this?.let { "\"$it\"" } ?: "null"

        // The backend omits the key rather than sending null, and so must the fixture: the response model
        // defaults it instead of accepting null.
        private fun String?.asOwnershipTypeEntry() = this?.let { ",\n\"ownership_type\": \"$it\"" }.orEmpty()
    }
}
