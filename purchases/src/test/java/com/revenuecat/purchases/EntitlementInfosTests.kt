package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.util.Iso8601Utils
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class EntitlementInfosTests {

    private var response = JSONObject()

    fun stubResponse(
        entitlements: JSONObject = JSONObject(),
        nonSubscriptions: JSONObject = JSONObject(),
        subscriptions: JSONObject = JSONObject()
    ) {
        response = JSONObject().apply {
            put("request_date", "2019-08-26T23:29:50Z")
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

    @Test
    fun `multiple entitlements`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
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
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        val subscriberInfo = response.buildPurchaserInfo()
        assertThat(subscriberInfo.entitlements.all).hasSize(2)

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct()

        verifyEntitlementActive(entitlement = "lifetime_cat")
        verifyRenewal(entitlement = "lifetime_cat")
        verifyPeriodType(entitlement = "lifetime_cat")
        verifyStore(entitlement = "lifetime_cat")
        verifySandbox(entitlement = "lifetime_cat")
        verifyProduct(
            identifier = "lifetime",
            latestPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            originalPurchaseDate = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
            expirationDate = null,
            entitlement = "lifetime_cat"
        )
    }

    @Test
    fun `string accessor`() {
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
                    put("expires_date", "2000-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "1999-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        val subscriberInfo = response.buildPurchaserInfo()
        assertThat(subscriberInfo.entitlements["pro_cat"]).isNotNull
        assertThat(subscriberInfo.entitlements.active["pro_cat"]).isNotNull
    }

    @Test
    fun `active subscription`() {
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

        verifyEntitlementActive()
    }

    @Test
    fun `inactive subscription`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2000-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2000-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "1999-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyEntitlementActive(false)
    }

    @Test
    fun `empty subscriber info`() {
        stubResponse()
        val subscriberInfo = response.buildPurchaserInfo()

        assertThat(subscriberInfo.firstSeen).isNotNull()
        assertThat(subscriberInfo.originalAppUserId).isEqualTo("cesarsandbox1")
        assertThat(subscriberInfo.entitlements.all).isEmpty()
    }

    @Test
    fun `creates entitlement infos`() {
        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
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
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifySubscriberInfo()
        verifyEntitlementActive()
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct(
            identifier = "lifetime",
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
                    put("product_identifier", "monthly_freetrial")
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
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at","2019-07-27T23:30:41Z")
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
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
                    put("product_identifier", "monthly_freetrial")
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
                    put("unsubscribe_detected_at", "2019-07-27T23:30:41Z")
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
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", "2019-07-27T22:30:41Z")
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", "2019-07-27T23:30:41Z")
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
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", true)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "normal")
                    put("purchase_date", "1999-07-26T23:30:41Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
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
        verifyRenewal()
        verifyPeriodType()
        verifyStore()
        verifySandbox()
        verifyProduct(
            identifier = "lifetime",
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
                    put("product_identifier", "monthly_freetrial")
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
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyStore(Store.APP_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
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
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "mac_app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyStore(Store.MAC_APP_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
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
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "play_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyStore(Store.PLAY_STORE)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
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
                    put("product_identifier", "monthly_freetrial")
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
                    put("product_identifier", "monthly_freetrial")
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
                    put("product_identifier", "monthly_freetrial")
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
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyPeriodType(PeriodType.NORMAL)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "intro")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyPeriodType(PeriodType.INTRO)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "trial")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
                })
            }
        )

        verifyPeriodType(PeriodType.TRIAL)

        stubResponse(
            entitlements = JSONObject().apply {
                put("pro_cat", JSONObject().apply {
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("product_identifier", "monthly_freetrial")
                    put("purchase_date", "2019-07-26T23:45:40Z")
                })
            },
            subscriptions = JSONObject().apply {
                put("monthly_freetrial", JSONObject().apply {
                    put("billing_issues_detected_at", JSONObject.NULL)
                    put("expires_date", "2200-07-26T23:50:40Z")
                    put("is_sandbox", false)
                    put("original_purchase_date", "2019-07-26T23:30:41Z")
                    put("period_type", "period")
                    put("purchase_date",  "2019-07-26T23:45:40Z")
                    put("store", "app_store")
                    put("unsubscribe_detected_at", JSONObject.NULL)
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
        val x = jsonObject.buildPurchaserInfo().entitlements
        val y = jsonObject.buildPurchaserInfo().entitlements
        assertThat(x.hashCode() == y.hashCode())
    }

    @Test
    fun `Given two same entitlementInfo, their hashcodes are the same`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)

        val all = jsonObject.buildPurchaserInfo().entitlements.all

        val x = all.values.toTypedArray()[0]
        val y = all.values.toTypedArray()[0]

        assertThat(x.hashCode() == y.hashCode())
    }

    private fun verifySubscriberInfo() {
        val subscriberInfo = response.buildPurchaserInfo()

        assertThat(subscriberInfo).isNotNull
        assertThat(subscriberInfo.firstSeen).isEqualTo(Iso8601Utils.parse("2019-07-26T23:29:50Z"))
        assertThat(subscriberInfo.originalAppUserId).isEqualTo("cesarsandbox1")
    }

    private fun verifyEntitlementActive(
        matcher: Boolean = true,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = response.buildPurchaserInfo()
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.identifier).isEqualTo(entitlement)
        assertThat(subscriberInfo.entitlements.all).containsKey(entitlement)
        assertThat(subscriberInfo.entitlements.active.keys.contains(entitlement)).isEqualTo(matcher)
        assertThat(proCat.isActive).isEqualTo(matcher)
    }

    private fun verifyRenewal(
        matcher: Boolean = true,
        unsubscribeDetectedAt: Date? = null,
        billingIssueDetectedAt: Date? = null,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = response.buildPurchaserInfo()
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.willRenew).isEqualTo(matcher)
        assertThat(proCat.unsubscribeDetectedAt).isEqualTo(unsubscribeDetectedAt)
        assertThat(proCat.billingIssueDetectedAt).isEqualTo(billingIssueDetectedAt)
    }

    private fun verifyPeriodType(
        matcher: PeriodType = PeriodType.NORMAL,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = response.buildPurchaserInfo()
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.periodType).isEqualTo(matcher)
    }

    private fun verifyStore(
        matcher: Store = Store.APP_STORE,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = response.buildPurchaserInfo()
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.store).isEqualTo(matcher)
    }

    private fun verifySandbox(
        matcher: Boolean = false,
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = response.buildPurchaserInfo()
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.isSandbox).isEqualTo(matcher)
    }

    private fun verifyProduct(
        identifier: String = "monthly_freetrial",
        latestPurchaseDate: Date = Iso8601Utils.parse("2019-07-26T23:45:40Z"),
        originalPurchaseDate: Date = Iso8601Utils.parse("2019-07-26T23:30:41Z"),
        expirationDate: Date? = Iso8601Utils.parse("2200-07-26T23:50:40Z"),
        entitlement: String = "pro_cat"
    ) {
        val subscriberInfo = response.buildPurchaserInfo()
        val proCat = subscriberInfo.entitlements[entitlement]!!

        assertThat(proCat.latestPurchaseDate).isEqualTo(latestPurchaseDate)
        assertThat(proCat.originalPurchaseDate).isEqualTo(originalPurchaseDate)
        assertThat(proCat.expirationDate).isEqualTo(expirationDate)
        assertThat(proCat.productIdentifier).isEqualTo(identifier)
    }
}