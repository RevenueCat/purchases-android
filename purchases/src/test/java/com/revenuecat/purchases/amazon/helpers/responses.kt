package com.revenuecat.purchases.amazon.helpers

fun successfulRVSResponse(
    termSku: String = "premium.subscription.quarterly"
) = """
        {
           "autoRenewing": true,
           "betaProduct": true,
           "cancelDate": null,
           "cancelReason": null,
           "deferredDate": null,
           "deferredSku": null,
           "freeTrialEndDate": null,
           "gracePeriodEndDate": null,
           "parentProductId": null,
           "productId": "premium.subscription",
           "productType": "SUBSCRIPTION",
           "purchaseDate": 1611819077000,
           "quantity": null,
           "receiptId": "hQ8uPyAdwptgdTLKGRhJ_smWRJQs0L0gj-Aejyah8uY=:3:24",
           "renewalDate": 1619595077000,
           "term": "3 Months",
           "termSku": "$termSku",
           "testTransaction": true
        }
    """.trimIndent()
