package com.revenuecat.purchases

import com.android.billingclient.api.BillingClient.BillingResponse.BILLING_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponse.DEVELOPER_ERROR
import com.android.billingclient.api.BillingClient.BillingResponse.ERROR
import com.android.billingclient.api.BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED
import com.android.billingclient.api.BillingClient.BillingResponse.ITEM_ALREADY_OWNED
import com.android.billingclient.api.BillingClient.BillingResponse.ITEM_NOT_OWNED
import com.android.billingclient.api.BillingClient.BillingResponse.ITEM_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponse.OK
import com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponse.USER_CANCELED

fun Int.getBillingResponseCodeName(): String {
    return when (this) {
        FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
        OK -> "OK"
        USER_CANCELED -> "USER_CANCELED"
        SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
        BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
        DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        ERROR -> "ERROR"
        ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
        ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
        else -> "$this"
    }
}