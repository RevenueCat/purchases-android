package com.revenuecat.purchases.galaxy.constants

// See https://developer.samsung.com/iap/programming-guide/iap-helper-programming.html#ErrorVo-and-Response-code
// for a full list of error codes. There is no documented error with an error code of -1014.
@SuppressWarnings("MagicNumber")
internal enum class GalaxyErrorCode(val code: Int) {
    IAP_ERROR_NONE(0),
    IAP_PAYMENT_IS_CANCELED(1),
    IAP_ERROR_INITIALIZATION(-1000),
    IAP_ERROR_NEED_APP_UPGRADE(-1001),
    IAP_ERROR_COMMON(-1002),
    IAP_ERROR_ALREADY_PURCHASED(-1003),
    IAP_ERROR_WHILE_RUNNING(-1004),
    IAP_ERROR_PRODUCT_DOES_NOT_EXIST(-1005),
    IAP_ERROR_CONFIRM_INBOX(-1006),
    IAP_ERROR_ITEM_GROUP_DOES_NOT_EXIST(-1007),
    IAP_ERROR_NETWORK_NOT_AVAILABLE(-1008),
    IAP_ERROR_IOEXCEPTION_ERROR(-1009),
    IAP_ERROR_SOCKET_TIMEOUT(-1010),
    IAP_ERROR_CONNECT_TIMEOUT(-1011),
    IAP_ERROR_NOT_EXIST_LOCAL_PRICE(-1012),
    IAP_ERROR_NOT_AVAILABLE_SHOP(-1013),
    IAP_ERROR_INVALID_ACCESS_TOKEN(-1015),
}
