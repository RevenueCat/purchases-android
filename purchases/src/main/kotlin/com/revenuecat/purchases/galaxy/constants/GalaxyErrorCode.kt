package com.revenuecat.purchases.galaxy.constants

@SuppressWarnings("MagicNumber")
internal enum class GalaxyErrorCode(val code: Int, val description: String) {
    IAP_ERROR_NONE(0, "Success"),
    IAP_PAYMENT_IS_CANCELED(1, "Payment canceled"),
}
