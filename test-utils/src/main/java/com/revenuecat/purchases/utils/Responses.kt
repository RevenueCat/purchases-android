package com.revenuecat.purchases.utils

import org.json.JSONObject
import java.util.Date

object Responses {
    @Suppress("LongMethod")
    fun createFullCustomerResponse(
        oneMonthFreeTrialExpirationDate: Date? = Iso8601Utils.parse("2100-04-06T20:54:45.975000Z"),
        threeMonthFreeTrialExpirationDate: Date? = Iso8601Utils.parse("1990-08-30T02:40:36Z"),
    ): String {
        return """
                {
                  "request_date": "2019-08-16T10:30:42Z",
                  "request_date_ms": 1565951442879,
                  "subscriber": {
                    "original_app_user_id": "app_user_id",
                    "original_application_version": "2083",
                    "first_seen": "2019-06-17T16:05:33Z",
                    "original_purchase_date": "2019-07-26T23:30:41Z",
                    "non_subscriptions": {
                      "100_coins_pack": [
                        {
                          "id": "72c26cc69c",
                          "is_sandbox": true,
                          "original_purchase_date": "1990-08-30T02:40:36Z",
                          "purchase_date": "1990-08-30T02:40:36Z",
                          "store": "app_store"
                        },
                        {
                          "id": "6229b0bef1",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-11-06T03:26:15Z",
                          "purchase_date": "2019-11-06T03:26:15Z",
                          "store": "play_store"
                        }
                      ],
                      "7_extra_lives": [
                        {
                          "id": "d6c007ba74",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-07-11T18:36:20Z",
                          "purchase_date": "2019-07-11T18:36:20Z",
                          "store": "play_store"
                        },
                        {
                          "id": "5b9ba226bc",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-07-26T22:10:27Z",
                          "purchase_date": "2019-07-26T22:10:27Z",
                          "store": "app_store"
                        }
                      ],
                      "lifetime_access": [
                        {
                          "id": "b6c007ba74",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-09-11T18:36:20Z",
                          "purchase_date": "2019-09-11T18:36:20Z",
                          "store": "play_store"
                        }
                      ]
                    },
                    "subscriptions": {
                      "pro": {
                        "billing_issues_detected_at": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2019-07-26T23:30:41Z",
                        "purchase_date": "2019-07-26T23:45:40Z",
                        "product_plan_identifier": "monthly",
                        "store": "app_store",
                        "unsubscribe_detected_at": null,
                        "expires_date": "${Iso8601Utils.format(oneMonthFreeTrialExpirationDate)}",
                        "period_type": "normal"
                      },
                      "basic": {
                        "billing_issues_detected_at": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2019-07-26T23:30:41Z",
                        "purchase_date": "2019-07-26T23:45:40Z",
                        "product_plan_identifier": "monthly",
                        "store": "app_store",
                        "unsubscribe_detected_at": null,
                        "period_type": "normal",
                        "expires_date": "${Iso8601Utils.format(threeMonthFreeTrialExpirationDate)}"
                      }
                    },
                    "entitlements": {
                      "pro": {
                        "expires_date": "2100-04-06T20:54:45.975000Z",
                        "product_identifier": "pro",
                        "product_plan_identifier": "monthly",
                        "purchase_date": "2018-10-26T23:17:53Z"
                      },
                      "basic": {
                        "expires_date": "1990-08-30T02:40:36Z",
                        "product_identifier": "basic",
                        "product_plan_identifier": "monthly",
                        "purchase_date": "1990-06-30T02:40:36Z"
                      },
                      "forever_pro": {
                        "expires_date": null,
                        "product_identifier": "lifetime_access",
                        "purchase_date": "2019-09-11T18:36:20Z"
                      }
                    },
                    "management_url": "https://play.google.com/store/account/subscriptions"
                  }
                }
            """.removeJSONFormatting()
    }

    val validFullPurchaserResponse = createFullCustomerResponse()

    val validEmptyPurchaserResponse = """
                {
                  "request_date": "2019-08-16T10:30:42Z",
                  "request_date_ms": 1565951442879,
                  "subscriber": {
                    "entitlements": {},
                    "first_seen": "2019-07-17T00:05:54Z",
                    "non_subscriptions": {},
                    "original_app_user_id": "",
                    "original_application_version": "1.0",
                    "other_purchases": {},
                    "subscriptions": {},
                    "management_url": null
                  }
                }
            """.removeJSONFormatting()

    val subscriberAttributesErrorsPostReceiptResponse = """
               {
                  "request_date": "2019-08-16T10:30:42Z",
                  "request_date_ms": 1565951442879,
                  "attributes_error_response": {
                    "attribute_errors": [{
                      "key_name": "invalid_name",
                      "message": "Attribute key name is not valid."
                    }],
                    "code": 7262,
                    "message": "Some subscriber attributes keys were unable to saved."
                  },
                  "subscriber": {
                    "original_app_user_id": "app_user_id",
                    "original_application_version": "2083",
                    "first_seen": "2019-06-17T16:05:33Z",
                    "non_subscriptions": {
                      "onetime_purchase": [
                        {
                          "id": "72c26cc69c",
                          "is_sandbox": true,
                          "original_purchase_date": "1990-08-30T02:40:36Z",
                          "purchase_date": "1990-08-30T02:40:36Z",
                          "store": "app_store"
                        }
                      ]
                    },
                    "subscriptions": {
                      "onemonth_freetrial": {
                        "billing_issues_detected_at": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2019-07-26T23:30:41Z",
                        "purchase_date": "2019-07-26T23:45:40Z",
                        "store": "app_store",
                        "unsubscribe_detected_at": null,
                        "expires_date": "2100-04-06T20:54:45.975000Z",
                        "period_type": "normal"
                      },
                      "threemonth_freetrial": {
                        "billing_issues_detected_at": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2019-07-26T23:30:41Z",
                        "purchase_date": "2019-07-26T23:45:40Z",
                        "store": "app_store",
                        "unsubscribe_detected_at": null,
                        "period_type": "normal",
                        "expires_date": "1990-08-30T02:40:36Z"
                      }
                    },
                    "entitlements": {
                      "pro": {
                        "expires_date": "2100-04-06T20:54:45.975000Z",
                        "product_identifier": "onemonth_freetrial",
                        "purchase_date": "2018-10-26T23:17:53Z"
                      },
                      "old_pro": {
                        "expires_date": "1990-08-30T02:40:36Z",
                        "product_identifier": "threemonth_freetrial",
                        "purchase_date": "1990-06-30T02:40:36Z"
                      },
                      "forever_pro": {
                        "expires_date": null,
                        "product_identifier": "onetime_purchase",
                        "purchase_date": "1990-08-30T02:40:36Z"
                      }
                    }
                  }
                } 
            """.removeJSONFormatting()

    val internalServerErrorResponse = """
                {
                    "code": 7110, 
                    "message": "Internal server error."
                }
            """.removeJSONFormatting()

    val invalidCredentialsErrorResponse = """
                {
                    "code": 7225, 
                    "message": "Invalid credentials error."
                }
            """.removeJSONFormatting()

    val badRequestErrorResponse = """
                {
                    "code": 7226, 
                    "message": "Missing required params."
                }
            """.removeJSONFormatting()
}

private fun String.removeJSONFormatting(): String = JSONObject(this).toString()
