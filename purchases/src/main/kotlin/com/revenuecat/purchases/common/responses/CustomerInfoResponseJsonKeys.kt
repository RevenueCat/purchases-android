package com.revenuecat.purchases.common.responses

internal object CustomerInfoResponseJsonKeys {
    const val REQUEST_DATE = "request_date"
    const val REQUEST_DATE_MS = "request_date_ms"
    const val SUBSCRIBER = "subscriber"
    const val ORIGINAL_APP_USER_ID = "original_app_user_id"
    const val ORIGINAL_APPLICATION_VERSION = "original_application_version"
    const val ENTITLEMENTS = "entitlements"
    const val FIRST_SEEN = "first_seen"
    const val ORIGINAL_PURCHASE_DATE = "original_purchase_date"
    const val NON_SUBSCRIPTIONS = "non_subscriptions"
    const val SUBSCRIPTIONS = "subscriptions"
    const val MANAGEMENT_URL = "management_url"
    const val PURCHASE_DATE = "purchase_date"
}

internal object EntitlementsResponseJsonKeys {
    const val EXPIRES_DATE = "expires_date"
    const val PRODUCT_IDENTIFIER = "product_identifier"
    const val PRODUCT_PLAN_IDENTIFIER = "product_plan_identifier"
    const val PURCHASE_DATE = "purchase_date"
}

internal object ProductResponseJsonKeys {
    const val BILLING_ISSUES_DETECTED_AT = "billing_issues_detected_at"
    const val IS_SANDBOX = "is_sandbox"
    const val ORIGINAL_PURCHASE_DATE = "original_purchase_date"
    const val PURCHASE_DATE = "purchase_date"
    const val PRODUCT_PLAN_IDENTIFIER = "product_plan_identifier"
    const val STORE = "store"
    const val UNSUBSCRIBE_DETECTED_AT = "unsubscribe_detected_at"
    const val EXPIRES_DATE = "expires_date"
    const val PERIOD_TYPE = "period_type"
    const val OWNERSHIP_TYPE = "ownership_type"
}
