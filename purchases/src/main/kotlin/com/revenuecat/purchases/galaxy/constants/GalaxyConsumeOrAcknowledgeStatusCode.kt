package com.revenuecat.purchases.galaxy.constants

// See https://developer.samsung.com/iap/programming-guide/iap-helper-programming.html#ConsumeVo
// for a full list of error codes.
@SuppressWarnings("MagicNumber")
internal enum class GalaxyConsumeOrAcknowledgeStatusCode(val code: Int) {
    SUCCESS(0),
    INVALID_PURCHASE_ID(1),
    FAILED_ORDER(2),
    INVALID_PRODUCT_TYPE(3),
    ALREADY_CONSUMED_OR_ACKNOWLEDGED(4),
    UNAUTHORIZED_USER(5),
    UNEXPECTED_SERVICE_ERROR(9),
    ;

    internal companion object {
        private val statusByCode: Map<Int, GalaxyConsumeOrAcknowledgeStatusCode> =
            values().associateBy { it.code }

        fun fromCode(code: Int): GalaxyConsumeOrAcknowledgeStatusCode? =
            statusByCode[code]
    }
}
