package com.revenuecat.purchases.common

import android.util.Log
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

fun debugLog(message: String) {
    if (Config.debugLogsEnabled) {
        Log.d("[Purchases] - DEBUG", message)
    }
}

fun infoLog(message: String) {
    Log.i("[Purchases] - INFO", message)
}

fun warnLog(message: String) {
    Log.w("[Purchases] - WARN", message)
}

fun errorLog(message: String) {
    Log.e("[Purchases] - ERROR", message)
}

fun errorLog(error: PurchasesError) {
    when (error.code) {
        PurchasesErrorCode.UnknownError,
        PurchasesErrorCode.NetworkError,
        PurchasesErrorCode.ReceiptAlreadyInUseError,
        PurchasesErrorCode.UnexpectedBackendResponseError,
        PurchasesErrorCode.InvalidAppUserIdError,
        PurchasesErrorCode.OperationAlreadyInProgressError,
        PurchasesErrorCode.UnknownBackendError,
        PurchasesErrorCode.InvalidSubscriberAttributesError -> log(LogIntent.RC_ERROR, error.message)
        PurchasesErrorCode.PurchaseCancelledError,
        PurchasesErrorCode.StoreProblemError,
        PurchasesErrorCode.PurchaseNotAllowedError,
        PurchasesErrorCode.PurchaseInvalidError,
        PurchasesErrorCode.ProductNotAvailableForPurchaseError,
        PurchasesErrorCode.ProductAlreadyPurchasedError,
        PurchasesErrorCode.InvalidReceiptError,
        PurchasesErrorCode.MissingReceiptFileError,
        PurchasesErrorCode.InvalidAppleSubscriptionKeyError,
        PurchasesErrorCode.IneligibleError,
        PurchasesErrorCode.InsufficientPermissionsError,
        PurchasesErrorCode.PaymentPendingError,
        PurchasesErrorCode.InvalidCredentialsError -> log(LogIntent.GOOGLE_ERROR, error.message)
    }
}
