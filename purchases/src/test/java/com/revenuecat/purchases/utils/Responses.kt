package com.revenuecat.purchases.utils

import org.json.JSONObject
import java.util.Date

public object Responses {
    @Suppress("LongMethod")
    public fun createFullCustomerResponse(
        oneMonthFreeTrialExpirationDate: Date? = Iso8601Utils.parse("2100-04-06T20:54:45.975000Z"),
        threeMonthFreeTrialExpirationDate: Date? = Iso8601Utils.parse("1990-08-30T02:40:36Z"),
        productsInfo: Map<String, Boolean> = mapOf("lifetime_access" to false),
    ): String {
        val productsInfoString = productsInfo
            .takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString(", ") { (productIdentifier, shouldConsume) ->
                """
                "$productIdentifier": {
                  "should_consume": $shouldConsume
                }
            """.trimIndent()
            }
            ?.let { "\"purchased_products\": {$it}," }
            ?: ""
        return """
                {
                  $productsInfoString
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
                          "store": "app_store",
                          "display_name": "100 Coins",
                          "price": {
                            "currency": "USD",
                            "amount": "0.99"
                          }
                        },
                        {
                          "id": "6229b0bef1",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-11-06T03:26:15Z",
                          "purchase_date": "2019-11-06T03:26:15Z",
                          "store": "play_store",
                          "display_name": "100 Coins",
                          "price": {
                            "currency": "USD",
                            "amount": "0.99"
                          }
                        }
                      ],
                      "7_extra_lives": [
                        {
                          "id": "d6c007ba74",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-07-11T18:36:20Z",
                          "purchase_date": "2019-07-11T18:36:20Z",
                          "store": "play_store",
                          "display_name": "7 Extra Lives",
                          "price": {
                            "currency": "USD",
                            "amount": "8.99"
                          }
                        },
                        {
                          "id": "5b9ba226bc",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-07-26T22:10:27Z",
                          "purchase_date": "2019-07-26T22:10:27Z",
                          "store": "app_store",
                          "display_name": "7 Extra Lives",
                          "price": {
                            "currency": "USD",
                            "amount": "8.99"
                          }
                        }
                      ],
                      "lifetime_access": [
                        {
                          "id": "b6c007ba74",
                          "is_sandbox": true,
                          "original_purchase_date": "2019-09-11T18:36:20Z",
                          "purchase_date": "2019-09-11T18:36:20Z",
                          "store": "play_store",
                          "display_name": "Lifetime Access",
                          "price": {
                            "currency": "USD",
                            "amount": "499"
                          }
                        }
                      ]
                    },
                    "subscriptions": {
                      "pro": {
                        "billing_issues_detected_at": null,
                        "grace_period_expires_date": null,
                        "auto_resume_date": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2019-07-26T23:30:41Z",
                        "purchase_date": "2019-07-26T23:45:40Z",
                        "product_plan_identifier": "monthly",
                        "store": "app_store",
                        "unsubscribe_detected_at": null,
                        "refunded_at": null,
                        "expires_date": "${Iso8601Utils.format(oneMonthFreeTrialExpirationDate)}",
                        "ownership_type": "PURCHASED",
                        "store_transaction_id" : "GPA.3394-7009-4518-41945..6",
                        "period_type": "normal",
                        "display_name": "Pro Monthly",
                        "price": {
                          "currency": "USD",
                          "amount": "4.99"
                        },
                        "management_url": "https://play.google.com/store/account/subscriptions"
                      },
                      "basic": {
                        "billing_issues_detected_at": null,
                        "grace_period_expires_date": null,
                        "auto_resume_date": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2019-07-26T23:30:41Z",
                        "purchase_date": "2019-07-26T23:45:40Z",
                        "product_plan_identifier": "monthly",
                        "store": "app_store",
                        "unsubscribe_detected_at": "2023-07-26T23:45:40Z",
                        "refunded_at": null,
                        "period_type": "normal",
                        "ownership_type": "PURCHASED",
                        "store_transaction_id" : "GPA.3394-7009-4518-41945..8",
                        "expires_date": "${Iso8601Utils.format(threeMonthFreeTrialExpirationDate)}",
                        "display_name": "Basic Monthly",
                        "price": {
                          "currency": "USD",
                          "amount": "5.99"
                        },
                        "management_url": "https://play.google.com/store/account/subscriptions"
                      },
                      "paused": {
                        "auto_resume_date": "2100-04-06T20:54:45.975000Z",
                        "billing_issues_detected_at": null,
                        "display_name": null,
                        "expires_date": "2023-04-04T13:46:05Z",
                        "grace_period_expires_date": null,
                        "is_sandbox": true,
                        "original_purchase_date": "2022-04-04T13:41:06Z",
                        "period_type": "normal",
                        "price": { "amount": 9.99, "currency": "USD" },
                        "product_plan_identifier": "monthly",
                        "purchase_date": "2022-04-04T13:41:06Z",
                        "refunded_at": null,
                        "store": "play_store",
                        "store_transaction_id": "GPA.3304-5067-2770-55157",
                        "unsubscribe_detected_at": null,
                        "management_url": "https://play.google.com/store/account/subscriptions"
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

    val validFullVirtualCurrenciesResponse = """
        {
          "virtual_currencies": {
            "COIN": {
              "balance": 1,
              "code": "COIN",
              "description": "It's a coin",
              "name": "Coin"
            },
            "RC_COIN": {
              "balance": 0,
              "code": "RC_COIN",
              "name": "RC Coin"
            }
          }
        }
    """.removeJSONFormatting()

    val validEmptyVirtualCurrenciesResponse = """
        {
          "virtual_currencies": {}
        }
    """.removeJSONFormatting()

    val validWebBillingProductsResponse = """
        {
            "product_details": [
                {
                    "current_price": {
                        "amount": 999,
                        "amount_micros": 9990000,
                        "currency": "EUR"
                    },
                    "default_purchase_option_id": "base_option",
                    "default_subscription_option_id": null,
                    "description": "A test monthly subscription product",
                    "identifier": "product1",
                    "normal_period_duration": "P1M",
                    "product_type": "subscription",
                    "purchase_options": {
                        "base_option": {
                            "base": {
                                "cycle_count": 1,
                                "period_duration": "P1M",
                                "price": {
                                    "amount": 999,
                                    "amount_micros": 9990000,
                                    "currency": "EUR"
                                }
                            },
                            "id": "base_option",
                            "intro_price": null,
                            "price_id": "test_price_id",
                            "trial": null
                        }
                    },
                    "subscription_options": {
                        "base_option": {
                            "base": {
                                "cycle_count": 1,
                                "period_duration": "P1M",
                                "price": {
                                    "amount": 999,
                                    "amount_micros": 9990000,
                                    "currency": "EUR"
                                }
                            },
                            "id": "base_option",
                            "intro_price": null,
                            "price_id": "test_price_id",
                            "trial": null
                        }
                    },
                    "title": "Test Monthly Subscription"
                },
                {
                    "current_price": {
                        "amount": 999,
                        "amount_micros": 9990000,
                        "currency": "EUR"
                    },
                    "default_purchase_option_id": "base_option",
                    "default_subscription_option_id": null,
                    "description": "A test monthly subscription product",
                    "identifier": "product2",
                    "normal_period_duration": "P1M",
                    "product_type": "subscription",
                    "purchase_options": {
                        "base_option": {
                            "base": {
                                "cycle_count": 1,
                                "period_duration": "P1M",
                                "price": {
                                    "amount": 999,
                                    "amount_micros": 9990000,
                                    "currency": "EUR"
                                }
                            },
                            "id": "base_option",
                            "intro_price": null,
                            "price_id": "test_price_id",
                            "trial": null
                        }
                    },
                    "subscription_options": {
                        "base_option": {
                            "base": {
                                "cycle_count": 1,
                                "period_duration": "P1M",
                                "price": {
                                    "amount": 999,
                                    "amount_micros": 9990000,
                                    "currency": "EUR"
                                }
                            },
                            "id": "base_option",
                            "intro_price": null,
                            "price_id": "test_price_id",
                            "trial": null
                        }
                    },
                    "title": "Test Monthly Subscription"
                }
            ]
        }
    """.removeJSONFormatting()
}

private fun String.removeJSONFormatting(): String = JSONObject(this).toString()
