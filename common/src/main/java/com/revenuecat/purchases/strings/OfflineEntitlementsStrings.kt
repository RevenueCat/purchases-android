package com.revenuecat.purchases.strings

object OfflineEntitlementsStrings {
    const val UPDATING_PRODUCT_ENTITLEMENT_MAPPING = "Product entitlement mappings are stale. Updating."
    const val SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS = "Successfully updated product entitlement mappings."
    const val ERROR_UPDATING_PRODUCT_ENTITLEMENTS = "Error updating product entitlement mappings. Error: %s."
    const val USING_OFFLINE_ENTITLEMENTS_CUSTOMER_INFO = "Using offline computed customer info. In this state " +
        "EntitlementInfo's isSandbox will always be false. Additionally, the expiration date may be inaccurate. " +
        "Once the transactions are synced with our backend, these values will be automatically corrected."
    const val RESETTING_OFFLINE_CUSTOMER_INFO_CACHE = "Resetting offline customer info cache."
    const val OFFLINE_ENTITLEMENTS_NOT_ENABLED = "Offline entitlements not enabled in this version."
    const val ALREADY_CALCULATING_OFFLINE_CUSTOMER_INFO = "Already calculating offline customer info for %s."
    const val ERROR_PARSING_PRODUCT_ENTITLEMENT_MAPPING = "Error parsing cached product entitlement mapping: %s"
    const val OFFLINE_ENTITLEMENTS_UNSUPPORTED_INAPP_PURCHASES = "Offline entitlements are not supported for " +
        "active inapp purchases. Found active inapp purchases."
    const val PRODUCT_ENTITLEMENT_MAPPING_REQUIRED = "Product entitlement mapping is required for offline " +
        "entitlements. Skipping offline customer info calculation."
}
