package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class EntitlementInfosTests {

    private var response = JSONObject()

    @Test
    fun `multiple entitlements`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
                put("lifetime_cat", JSONObject().apply {
                    put("expires_date", JSONObject.NULL)
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "app_store")
                    })
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        val subscriberInfo = createCustomerInfo(response, null, VerificationResult.VERIFIED)
        assertThat(subscriberInfo.entitlements.all).hasSize(2)
        assertThat(subscriberInfo.entitlements.verification).isEqualTo(VerificationResult.VERIFIED)
        subscriberInfo.entitlements.all.onEach { entry ->
            assertThat(entry.value.verification).isEqualTo(VerificationResult.VERIFIED)
        }

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()

        verifyEntitlementActive(entitlement = "lifetime_cat")
        verifyRenewal(willRenew = false, entitlement = "lifetime_cat")
        verifyPeriodType(entitlement = "lifetime_cat")
        verifyStore(entitlement = "lifetime_cat")
        verifySandbox(entitlement = "lifetime_cat")
        verifyProduct(
            identifier = "lifetime",
            planIdentifier = null,
            latestPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            originalPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            expirationDate = null,
            entitlement = "lifetime_cat"
        )
    }

    @Test
    fun `verification error is set correctly for entitlementInfos and entitlementInfo`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "1999-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        val subscriberInfo = createCustomerInfo(response, null, VerificationResult.FAILED)
        assertThat(subscriberInfo.entitlements.verification).isEqualTo(VerificationResult.FAILED)
        assertThat(subscriberInfo.entitlements["pro_cat"]?.verification).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `string accessor`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2000-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "1999-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        val subscriberInfo = createCustomerInfo(response)
        assertThat(subscriberInfo.entitlements["pro_cat"]).isNotNull
        assertThat(subscriberInfo.entitlements.active["pro_cat"]).isNotNull
    }

    @Test
    fun `active subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "1999-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyEntitlementActive()
    }

    @Test
    fun `inactive subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2000-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2000-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "1999-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyEntitlementActive(false)
    }

    @Test
    fun `empty subscriber info`() {
        stubResponse()
        val subscriberInfo = createCustomerInfo(response)

        assertThat(subscriberInfo.firstSeen).isNotNull
        assertThat(subscriberInfo.originalAppUserId).isEqualTo("cesarsandbox1")
        assertThat(subscriberInfo.entitlements.all).isEmpty()
    }

    @Test
    fun `creates entitlement infos`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()
    }

    @Test
    fun `creates entitlement infos with non subscription and subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", JSONObject.NULL)
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "app_store")
                    })
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal(willRenew = false)
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct(
            identifier = "lifetime",
            planIdentifier = null,
            latestPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            originalPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            expirationDate = null
        )
    }

    @Test
    fun `subscription will renew`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()
    }

    @Test
    fun `subscription wont renew billing error`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at","2019-07-27T23:30:41Z")
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal(false, billingIssueDetectedAt = Iso8601Utils.parse("2019-07-27T23:30:41Z"))
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()
    }

    @Test
    fun `subscription wont renew cancelled`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", "2019-07-27T23:30:41Z")
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal(false, unsubscribeDetectedAt = Iso8601Utils.parse("2019-07-27T23:30:41Z"))
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()
    }

    @Test
    fun `subscription wont renew billing error and cancelled`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", "2019-07-27T22:30:41Z")
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", "2019-07-27T23:30:41Z")
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal(
            false,
            billingIssueDetectedAt = Iso8601Utils.parse("2019-07-27T22:30:41Z"),
            unsubscribeDetectedAt = Iso8601Utils.parse("2019-07-27T23:30:41Z")
        )
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()

    }

    @Test
    fun `subscription is sandbox`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", true)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox(true)
        verifyProduct()
    }

    @Test
    fun `non subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", JSONObject.NULL)
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "app_store")
                    })
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal(willRenew = false)
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct(
            identifier = "lifetime",
            planIdentifier = null,
            latestPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            originalPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            expirationDate = null)
    }

    @Test
    fun `store from subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyStore(Store.APP_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "mac_app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyStore(Store.MAC_APP_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "play_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyStore(Store.PLAY_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "promotional")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyStore(Store.PROMOTIONAL)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "stripe")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyStore(Store.STRIPE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "tienda")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyStore(Store.UNKNOWN_STORE)
    }

    @Test
    fun `store from non subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", JSONObject.NULL)
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "app_store")
                    })
                })
            }
        )

        verifyStore(Store.APP_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "mac_app_store")
                    })
                })
            }
        )

        verifyStore(Store.MAC_APP_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "play_store")
                    })
                })
            }
        )

        verifyStore(Store.PLAY_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "promotional")
                    })
                })
            }
        )

        verifyStore(Store.PROMOTIONAL)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "stripe")
                    })
                })
            }
        )

        verifyStore(Store.STRIPE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "tienda")
                    })
                })
            }
        )

        verifyStore(Store.UNKNOWN_STORE)
    }

    @Test
    fun `period from subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyPeriodType(PeriodType.NORMAL)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "intro")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyPeriodType(PeriodType.INTRO)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "trial")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyPeriodType(PeriodType.TRIAL)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "period")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyPeriodType(PeriodType.NORMAL)
    }

    @Test
    fun `period from non subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", JSONObject.NULL)
                    put("product_identifier", "lifetime")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            nonSubscriptions = JSONObject().apply {
                put("lifetime", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "5b9ba226bc")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T22:10:27Z")
                        put("purchase_date", "2019-07-26T22:10:27Z")
                        put("store", "app_store")
                    })
                    put(JSONObject().apply {
                        put("id", "ea820afcc4")
                        put("is_sandbox", false)
                        put("original_purchase_date", "2019-07-26T23:45:40Z")
                        put("purchase_date", "2019-07-26T23:45:40Z")
                        put("store", "app_store")
                    })
                })
            }
        )

        verifyPeriodType(PeriodType.NORMAL)
    }

    @Test
    fun `Given two same entitlementInfos, their hashcodes are the same`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = createCustomerInfo(jsonObject).entitlements
        val y = createCustomerInfo(jsonObject).entitlements
        assertThat(x.hashCode()).isEqualTo(y.hashCode())
    }

    @Test
    fun `Given two same entitlementInfo, their hashcodes are the same`() {
        val all = createCustomerInfo(Responses.validFullPurchaserResponse).entitlements.all

        val x = all.values.toTypedArray()[0]
        val y = all.values.toTypedArray()[0]

        assertThat(x.hashCode()).isEqualTo(y.hashCode())
    }

    @Test
    fun `promos won't renew`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2221-01-10T02:35:25Z")
                    put("product_identifier", "rc_promo_pro_cat_lifetime")
                    put("purchase_date", "2021-02-27T02:35:25Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("rc_promo_pro_cat_lifetime", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2221-01-10T02:35:25Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2021-02-27T02:35:25Z")
                    put("period_type", "normal")
                    put("purchase_date", "2021-02-27T02:35:25Z")
                    put("store", "promotional")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyRenewal(willRenew = false)
    }

    @Test
    fun `ownershipType from subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("ownership_type", "PURCHASED")
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyOwnershipType(OwnershipType.PURCHASED)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "intro")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("ownership_type", "FAMILY_SHARED")
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyOwnershipType(OwnershipType.FAMILY_SHARED)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "trial")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("ownership_type", "UNKNOWN")
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyOwnershipType(OwnershipType.UNKNOWN)
    }

    @Test
    fun `unknown ownershipType from subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("ownership_type", "AN_UNKNOWN_OWNERSHIP_TYPE")
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyOwnershipType(OwnershipType.UNKNOWN)
    }

    @Test
    fun `missing ownershipType from subscription maps to UNKNOWN`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "pro")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("pro", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            }
        )

        verifyOwnershipType(OwnershipType.UNKNOWN)
    }

    // region Equality tests

    @Test
    fun `same entitlement infos with different verifications are not equal`() {
        val entitlementInfos1 = EntitlementInfos(emptyMap(), VerificationResult.NOT_REQUESTED)
        val entitlementInfos2 = EntitlementInfos(emptyMap(), VerificationResult.VERIFIED)
        val entitlementInfos3 = EntitlementInfos(emptyMap(), VerificationResult.FAILED)
        val entitlementInfos4 = EntitlementInfos(emptyMap(), VerificationResult.VERIFIED_ON_DEVICE)
        assertThat(entitlementInfos1).isNotEqualTo(entitlementInfos2)
        assertThat(entitlementInfos1).isNotEqualTo(entitlementInfos3)
        assertThat(entitlementInfos1).isNotEqualTo(entitlementInfos4)
        assertThat(entitlementInfos2).isNotEqualTo(entitlementInfos3)
        assertThat(entitlementInfos2).isNotEqualTo(entitlementInfos4)
        assertThat(entitlementInfos3).isNotEqualTo(entitlementInfos4)
    }

    @Test
    fun `same entitlement infos with same verifications are equal`() {
        val entitlementInfos1 = EntitlementInfos(emptyMap(), VerificationResult.NOT_REQUESTED)
        val entitlementInfos2 = EntitlementInfos(emptyMap(), VerificationResult.NOT_REQUESTED)
        assertThat(entitlementInfos1).isEqualTo(entitlementInfos2)
    }

    // endregion

    // region EntitlementInfo active with grace periods

    @Test
    fun `entitlementInfo is active if within grace period`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", Iso8601Utils.format(1.days.ago()))
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("product_plan_identifier", "monthly")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            },
            requestDate = 2.days.ago()
        )

        val customerInfo = createCustomerInfo(response)
        assertThat(customerInfo.entitlements.all.size).isEqualTo(1)
        assertThat(customerInfo.entitlements.all.containsKey("pro_cat")).isTrue
        assertThat(customerInfo.entitlements.all["pro_cat"]?.isActive).isTrue
    }

    @Test
    fun `entitlementInfo is not active if past grace period`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", Iso8601Utils.format(1.days.ago()))
                    put("product_identifier", "monthly_freetrial")
                    put("product_plan_identifier", "monthly")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            },
            requestDate = 5.days.ago()
        )

        val customerInfo = createCustomerInfo(response)
        assertThat(customerInfo.entitlements.all.size).isEqualTo(1)
        assertThat(customerInfo.entitlements.all.containsKey("pro_cat")).isTrue
        assertThat(customerInfo.entitlements.all["pro_cat"]?.isActive).isFalse
    }

    @Test
    fun `entitlementInfo is active if expiration time in the future`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", Iso8601Utils.format(1.days.fromNow()))
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("product_plan_identifier", "monthly")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                    put("product_plan_identifier", "monthly")
                })
            },
            requestDate = 5.days.ago()
        )

        val customerInfo = createCustomerInfo(response)
        assertThat(customerInfo.entitlements.all.size).isEqualTo(1)
        assertThat(customerInfo.entitlements.all.containsKey("pro_cat")).isTrue
        assertThat(customerInfo.entitlements.all["pro_cat"]?.isActive).isTrue
    }

    // endregion

    // region Helpers

    private fun verifySubscriberInfo() {
        val subscriberInfo = createCustomerInfo(response)

        assertThat(subscriberInfo).isNotNull
        assertThat(subscriberInfo.firstSeen).isEqualTo(Iso8601Utils.parse("2019-07-26T23:29:50Z"))
        assertThat(subscriberInfo.originalAppUserId).isEqualTo("cesarsandbox1")
    }

    private fun verifyEntitlementActive(
        matcher: Boolean = true,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.identifier).isEqualTo(entitlement)
        assertThat(subscriberInfo.entitlements.all).containsKey(entitlement)
        assertThat(subscriberInfo.entitlements.active.keys.contains(entitlement)).isEqualTo(matcher)
        assertThat(proCat.isActive).isEqualTo(matcher)
    }

    private fun verifyRenewal(
        willRenew: Boolean = true,
        unsubscribeDetectedAt: Date? = null,
        billingIssueDetectedAt: Date? = null,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.willRenew).isEqualTo(willRenew)
        assertThat(proCat.unsubscribeDetectedAt).isEqualTo(unsubscribeDetectedAt)
        assertThat(proCat.billingIssueDetectedAt).isEqualTo(billingIssueDetectedAt)
    }

    private fun verifyPeriodType(
        matcher: PeriodType = PeriodType.NORMAL,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.periodType).isEqualTo(matcher)
    }

    private fun verifyStore(
        matcher: Store = Store.APP_STORE,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.store).isEqualTo(matcher)
    }

    private fun verifyOwnershipType(
        matcher: OwnershipType = OwnershipType.PURCHASED,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.ownershipType).isEqualTo(matcher)
    }

    private fun verifySandbox(
        matcher: Boolean = false,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.isSandbox).isEqualTo(matcher)
    }

    private fun verifyProduct(
        identifier: String = "pro",
        planIdentifier: String? = "monthly",
        latestPurchaseDate: Date = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
        originalPurchaseDate: Date = Iso8601Utils.parse("2019-07-26T23:30:41Z"),
        expirationDate: Date? = Iso8601Utils.parse("2200-07-26T23:50:40Z"),
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = createCustomerInfo(response)
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.latestPurchaseDate).isEqualTo(latestPurchaseDate)
        assertThat(proCat.originalPurchaseDate).isEqualTo(originalPurchaseDate)
        assertThat(proCat.expirationDate).isEqualTo(expirationDate)
        assertThat(proCat.productIdentifier).isEqualTo(identifier)
        assertThat(proCat.productPlanIdentifier).isEqualTo(planIdentifier)
    }

    private fun stubResponse(
        entitlements: JSONObject = JSONObject(),
        nonSubscriptions: JSONObject = JSONObject(),
        subscriptions: JSONObject = JSONObject(),
        requestDate: Date = Iso8601Utils.parse("2019-08-26T23:29:50Z")
    ) {
        response = JSONObject().apply {
            put("request_date", Iso8601Utils.format(requestDate))
            put("subscriber", JSONObject().apply {
                put("entitlements", entitlements)
                put("first_seen", "2019-07-26T23:29:50Z")
                put("non_subscriptions", nonSubscriptions)
                put("original_app_user_id", "cesarsandbox1")
                put("original_application_version", "1.0")
                put("subscriptions", subscriptions)
            })
        }
    }

    // endregion
}
