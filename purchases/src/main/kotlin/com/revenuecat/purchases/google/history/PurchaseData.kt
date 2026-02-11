package com.revenuecat.purchases.google.history

import com.revenuecat.purchases.common.errorLog
import org.json.JSONObject

/**
 * Data class representing a purchase from the Play Store.
 * This is used to parse the JSON strings returned by the AIDL getPurchaseHistory call.
 */
internal data class PurchaseData(
    public val orderId: String,
    public val packageName: String,
    public val productId: String,
    public val purchaseTime: Long,
    public val purchaseState: Int,
    public val purchaseToken: String,
    public val quantity: Int,
    public val acknowledged: Boolean,
    public val autoRenewing: Boolean,
) {
    public companion object {
        /**
         * Parse a purchase data JSON string into a PurchaseData object.
         */
        public fun fromJson(json: String): PurchaseData? {
            return try {
                val jsonObject = JSONObject(json)
                PurchaseData(
                    orderId = jsonObject.optString("orderId", ""),
                    packageName = jsonObject.optString("packageName", ""),
                    productId = jsonObject.optString("productId", ""),
                    purchaseTime = jsonObject.optLong("purchaseTime", 0),
                    purchaseState = jsonObject.optInt("purchaseState", 0),
                    purchaseToken = jsonObject.optString("purchaseToken", ""),
                    quantity = jsonObject.optInt("quantity", 1),
                    acknowledged = jsonObject.optBoolean("acknowledged", false),
                    autoRenewing = jsonObject.optBoolean("autoRenewing", false),
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                errorLog(e) { "Error parsing AIDL purchase data JSON: $json" }
                null
            }
        }
    }
}
