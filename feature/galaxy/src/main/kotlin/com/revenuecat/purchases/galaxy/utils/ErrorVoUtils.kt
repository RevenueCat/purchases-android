package com.revenuecat.purchases.galaxy.utils

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.samsung.android.sdk.iap.lib.vo.ErrorVo

internal fun ErrorVo.isError(): Boolean {
    return this.errorCode != GalaxyErrorCode.IAP_ERROR_NONE.code
}

internal fun ErrorVo.toPurchasesError(): PurchasesError {
    val galaxyErrorCodesByCode = GalaxyErrorCode.values().associateBy { it.code }
    val galaxyErrorCode = galaxyErrorCodesByCode[this.errorCode]

    val purchasesErrorCode = when (galaxyErrorCode) {
        GalaxyErrorCode.IAP_PAYMENT_IS_CANCELED -> PurchasesErrorCode.PurchaseCancelledError
        GalaxyErrorCode.IAP_ERROR_ALREADY_PURCHASED -> PurchasesErrorCode.ProductAlreadyPurchasedError
        GalaxyErrorCode.IAP_ERROR_PRODUCT_DOES_NOT_EXIST,
        GalaxyErrorCode.IAP_ERROR_ITEM_GROUP_DOES_NOT_EXIST,
        GalaxyErrorCode.IAP_ERROR_NOT_EXIST_LOCAL_PRICE,
        -> PurchasesErrorCode.ProductNotAvailableForPurchaseError
        GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE,
        GalaxyErrorCode.IAP_ERROR_IOEXCEPTION_ERROR,
        GalaxyErrorCode.IAP_ERROR_SOCKET_TIMEOUT,
        GalaxyErrorCode.IAP_ERROR_CONNECT_TIMEOUT,
        -> PurchasesErrorCode.NetworkError
        GalaxyErrorCode.IAP_ERROR_WHILE_RUNNING -> PurchasesErrorCode.PurchaseInvalidError
        GalaxyErrorCode.IAP_ERROR_NEED_APP_UPGRADE,
        GalaxyErrorCode.IAP_ERROR_INITIALIZATION,
        GalaxyErrorCode.IAP_ERROR_COMMON,
        GalaxyErrorCode.IAP_ERROR_CONFIRM_INBOX,
        -> PurchasesErrorCode.StoreProblemError
        GalaxyErrorCode.IAP_ERROR_NOT_AVAILABLE_SHOP -> PurchasesErrorCode.PurchaseNotAllowedError
        GalaxyErrorCode.IAP_ERROR_INVALID_ACCESS_TOKEN -> PurchasesErrorCode.InvalidCredentialsError
        GalaxyErrorCode.IAP_ERROR_NONE -> {
            log(LogIntent.GALAXY_WARNING) { GalaxyStrings.CREATING_PURCHASES_ERROR_FOR_GALAXY_ERROR_NONE }
            PurchasesErrorCode.UnknownError
        }
        null -> PurchasesErrorCode.UnknownError
    }

    return PurchasesError(
        code = purchasesErrorCode,
        underlyingErrorMessage = this.errorString,
    )
}
