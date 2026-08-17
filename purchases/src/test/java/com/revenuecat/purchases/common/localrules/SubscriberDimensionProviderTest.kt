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
class SubscriberDimensionProviderTest {

    private val date = Date(1_700_000_000_000)

    @Test
    fun `provides the subscriber dimensions`() = runTest {
        val dimensions = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date)

        assertThat(dimensions).isEqualTo(
            mapOf(
                "customerId" to string("current_user"),
                "originalAppUserId" to string("original_user"),
                "allPurchasedProductIds" to list("coins", "legacy:annual", "premium:monthly"),
                "anyActiveStore" to list("amazon", "play_store"),
                "latestEntitlements" to list("extra", "premium"),
                "latestStore" to string("play_store"),
                "latestOwnershipType" to string("PURCHASED"),
                "isCurrentlyTrialing" to bool(false),
                "isRcPromo" to bool(false),
                "hasMadeNonSubscriptionPurchase" to bool(true),
                "hasMadeSandboxPurchase" to bool(true),
                "latestAutoRenewIntent" to bool(true),
                "firstSeenAt" to date("2021-01-01T00:00:00Z"),
                "lastSeenAt" to date("2024-06-01T00:00:00Z"),
                "latestExpirationAt" to date("2100-01-01T00:00:00Z"),
                "mostRecentPurchaseAt" to date("2024-01-15T10:00:00Z"),
                "mostRecentRenewalAt" to date("2024-01-15T10:00:00Z"),
            ),
        )
    }

    @Test
    fun `a customer who has never purchased answers the questions that have an answer`() = runTest {
        val dimensions = provider(customerInfo(Responses.validEmptyPurchaserResponse)).dimensions(date)

        assertThat(dimensions).isEqualTo(
            mapOf(
                "customerId" to string("current_user"),
                "isCurrentlyTrialing" to bool(false),
                "isRcPromo" to bool(false),
                "hasMadeNonSubscriptionPurchase" to bool(false),
                "hasMadeSandboxPurchase" to bool(false),
                "firstSeenAt" to date("2019-07-17T00:05:54Z"),
                "lastSeenAt" to date("2019-08-16T10:30:42Z"),
            ),
        )
    }

    @Test
    fun `a trial reports its window and keeps its opt-out apart from a subscription's`() = runTest {
        val dimensions = provider(customerInfo(TRIALING_RESPONSE)).dimensions(date)

        assertThat(dimensions["isCurrentlyTrialing"]).isEqualTo(bool(true))
        assertThat(dimensions["trialStartAt"]).isEqualTo(date("2024-05-01T00:00:00Z"))
        assertThat(dimensions["trialEndAt"]).isEqualTo(date("2100-01-01T00:00:00Z"))
        assertThat(dimensions["trialOptOutAt"]).isEqualTo(date("2024-05-03T00:00:00Z"))
        assertThat(dimensions).doesNotContainKey("subscriptionOptOutAt")
        // An unsubscribe is exactly what says the renewal is not coming.
        assertThat(dimensions["latestAutoRenewIntent"]).isEqualTo(bool(false))
    }

    @Test
    fun `a paid subscription's opt-out is kept apart from a trial's`() = runTest {
        val dimensions = provider(customerInfo(UNSUBSCRIBED_RESPONSE)).dimensions(date)

        assertThat(dimensions["subscriptionOptOutAt"]).isEqualTo(date("2024-05-03T00:00:00Z"))
        assertThat(dimensions).doesNotContainKey("trialOptOutAt")
        assertThat(dimensions["isCurrentlyTrialing"]).isEqualTo(bool(false))
    }

    @Test
    fun `an entitlement granted through RevenueCat is reported as a promo`() = runTest {
        val dimensions = provider(customerInfo(PROMO_RESPONSE)).dimensions(date)

        assertThat(dimensions["isRcPromo"]).isEqualTo(bool(true))
        assertThat(dimensions["latestStore"]).isEqualTo(string("promotional"))
        // A promo never renews.
        assertThat(dimensions["latestAutoRenewIntent"]).isEqualTo(bool(false))
    }

    @Test
    fun `an unknown ownership type is reported as no ownership type at all`() = runTest {
        val dimensions = provider(customerInfo(PROMO_RESPONSE)).dimensions(date)

        assertThat(dimensions).doesNotContainKey("latestOwnershipType")
    }

    @Test
    fun `family sharing is reported when the latest purchase is shared`() = runTest {
        val dimensions = provider(customerInfo(FAMILY_SHARED_RESPONSE)).dimensions(date)

        assertThat(dimensions["latestOwnershipType"]).isEqualTo(string("FAMILY_SHARED"))
    }

    @Test
    fun `a customer info that cannot be read leaves the other dimensions usable`() = runTest {
        // Not just PurchasesException: the read goes through the configured instance, which an app can tear down
        // mid-evaluation, and it can fall back to the network.
        val failures = listOf(
            PurchasesException(PurchasesError(PurchasesErrorCode.NetworkError, "Nope.")),
            UninitializedPropertyAccessException("There is no singleton instance."),
            IllegalStateException("Something else entirely."),
        )

        for (failure in failures) {
            val snapshot = RulesDimensionResolver(
                providers = listOf(
                    deviceProvider(),
                    SubscriberDimensionProvider(appUserId = { "current_user" }, customerInfo = { throw failure }),
                ),
            ).snapshot()

            assertThat(snapshot.isSuccess).describedAs("%s", failure).isTrue()
            // The app user ID is known without asking the backend, so a rule on it survives the failure.
            assertThat(snapshot.getOrThrow().values["subscriber"]).describedAs("%s", failure).isEqualTo(
                Value.ObjectValue(mapOf("customerId" to Value.StringValue("current_user"))),
            )
        }
    }

    @Test
    fun `an unknown app user ID leaves the subscriber dimensions usable`() = runTest {
        val provider = SubscriberDimensionProvider(
            appUserId = { throw UninitializedPropertyAccessException("There is no singleton instance.") },
            customerInfo = { customerInfo(SUBSCRIBED_RESPONSE) },
        )

        val dimensions = provider.dimensions(date)

        assertThat(dimensions).doesNotContainKey("customerId")
        assertThat(dimensions["originalAppUserId"]).isEqualTo(string("original_user"))
    }

    @Test
    fun `cancellation while reading the customer info propagates`() = runTest {
        val cancelling = SubscriberDimensionProvider(
            appUserId = { "current_user" },
            customerInfo = { throw CancellationException("cancelled") },
        )

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
        val provider = SubscriberDimensionProvider(
            appUserId = { "current_user" },
            customerInfo = { customerInfo(response) },
        )

        assertThat(provider.dimensions(date)["hasMadeNonSubscriptionPurchase"]).isEqualTo(bool(false))

        response = SUBSCRIBED_RESPONSE

        assertThat(provider.dimensions(date)["hasMadeNonSubscriptionPurchase"]).isEqualTo(bool(true))
    }

    @Test
    fun `a subscription in a grace period still counts as bought from its store`() = runTest {
        // "legacy:annual" expired in 2023 but its grace period runs to 2100.
        val dimensions = provider(customerInfo(SUBSCRIBED_RESPONSE)).dimensions(date)

        assertThat(dimensions["anyActiveStore"]).isEqualTo(list("amazon", "play_store"))
    }

    @Test
    fun `subscriber dimensions are reachable by dot-path from a predicate`() = runTest {
        val values = RulesDimensionResolver(providers = listOf(provider(customerInfo(SUBSCRIBED_RESPONSE))))
            .snapshot()
            .getOrThrow()
            .values

        val predicates = listOf(
            """{"==": [{"var": "subscriber.customerId"}, "current_user"]}""",
            """{">": [{"var": "subscriber.latestExpirationAt"}, 1700000000000]}""",
            """{"in": ["premium", {"var": "subscriber.latestEntitlements"}]}""",
            """{"!": [{"var": "subscriber.isCurrentlyTrialing"}]}""",
        )

        for (predicate in predicates) {
            assertThat(RulesEngine.evaluate(predicate, values).getOrThrow()).describedAs(predicate).isTrue()
        }
    }

    private fun provider(customerInfo: CustomerInfo) = SubscriberDimensionProvider(
        appUserId = { "current_user" },
        customerInfo = { customerInfo },
    )

    private fun deviceProvider() = object : RulesDimensionProvider {
        override val identifier = "device"
        override val namespace = RulesDimensionNamespace.Device
        override suspend fun dimensions(date: Date) = mapOf("platform" to string("android"))
    }

    private fun customerInfo(response: String): CustomerInfo =
        CustomerInfoFactory.buildCustomerInfo(JSONObject(response), null, VerificationResult.NOT_REQUESTED)

    private fun string(value: String) = RulesDimensionValue.StringValue(value)
    private fun bool(value: Boolean) = RulesDimensionValue.BoolValue(value)
    private fun list(vararg values: String) = RulesDimensionValue.StringListValue(values.toList())
    private fun date(iso8601: String) = RulesDimensionValue.DateValue(Iso8601Utils.parse(iso8601))

    private companion object {

        /**
         * A Google subscription bought most recently, an Amazon one that expired but is in a grace period, and a
         * one-time purchase. Two entitlements are unlocked by the latest subscription and one by the older one.
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

        val TRIALING_RESPONSE = subscriberResponse(
            subscriptions = """
                "premium": {
                  ${subscription(
                store = "play_store",
                purchaseDate = "2024-05-01T00:00:00Z",
                originalPurchaseDate = "2024-05-01T00:00:00Z",
                expiresDate = "2100-01-01T00:00:00Z",
                ownershipType = "PURCHASED",
                periodType = "trial",
                unsubscribeDetectedAt = "2024-05-03T00:00:00Z",
            )}
                }
            """,
        )

        val UNSUBSCRIBED_RESPONSE = subscriberResponse(
            subscriptions = """
                "premium": {
                  ${subscription(
                store = "play_store",
                purchaseDate = "2024-05-01T00:00:00Z",
                originalPurchaseDate = "2024-05-01T00:00:00Z",
                expiresDate = "2100-01-01T00:00:00Z",
                ownershipType = "PURCHASED",
                unsubscribeDetectedAt = "2024-05-03T00:00:00Z",
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

        val FAMILY_SHARED_RESPONSE = subscriberResponse(
            subscriptions = """
                "premium": {
                  ${subscription(
                store = "app_store",
                purchaseDate = "2024-05-01T00:00:00Z",
                originalPurchaseDate = "2024-05-01T00:00:00Z",
                expiresDate = "2100-01-01T00:00:00Z",
                ownershipType = "FAMILY_SHARED",
            )}
                }
            """,
        )

        @Suppress("LongParameterList")
        private fun subscription(
            store: String,
            purchaseDate: String,
            originalPurchaseDate: String,
            expiresDate: String,
            ownershipType: String?,
            productPlanIdentifier: String? = "monthly",
            periodType: String = "normal",
            unsubscribeDetectedAt: String? = null,
            gracePeriodExpiresDate: String? = null,
        ) = """
            "store": "$store",
            "product_plan_identifier": ${productPlanIdentifier.asJsonString()},
            "purchase_date": "$purchaseDate",
            "original_purchase_date": "$originalPurchaseDate",
            "expires_date": "$expiresDate",
            "period_type": "$periodType",
            "is_sandbox": true,
            "unsubscribe_detected_at": ${unsubscribeDetectedAt.asJsonString()},
            "billing_issues_detected_at": null,
            "grace_period_expires_date": ${gracePeriodExpiresDate.asJsonString()},
            "auto_resume_date": null,
            "refunded_at": null,
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
