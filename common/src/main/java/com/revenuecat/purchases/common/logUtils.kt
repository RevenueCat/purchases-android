package com.revenuecat.purchases.common

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.logging.LogLevel

fun debugLog(message: String, elements: Array<StackTraceElement>? = null) {
    if (Config.debugLogsEnabled) {
        currentVerboseLogHandler.log(LogLevel.DEBUG, LogLevel.DEBUG.createTag(), message, null, elements)
    }
}

fun infoLog(message: String, elements: Array<StackTraceElement>? = null) {
    currentVerboseLogHandler.log(LogLevel.INFO, LogLevel.INFO.createTag(), message, null, elements)
}

fun warnLog(message: String, elements: Array<StackTraceElement>? = null) {
    currentVerboseLogHandler.log(LogLevel.WARN, LogLevel.WARN.createTag(), message, null, elements)
}

fun errorLog(message: String, throwable: Throwable? = null, elements: Array<StackTraceElement>? = null) {
    currentVerboseLogHandler.log(LogLevel.ERROR, LogLevel.ERROR.createTag(), message, throwable, elements)
}

fun verboseLog(message: String, throwable: Throwable? = null, elements: Array<StackTraceElement>? = null) {
    currentVerboseLogHandler.log(LogLevel.VERBOSE, LogLevel.VERBOSE.createTag(), message, throwable, elements)
}

fun LogLevel.createTag() = "[Purchases] - ${this.name}"

fun errorLog(error: PurchasesError) {
    when (error.code) {
        PurchasesErrorCode.UnknownError,
        PurchasesErrorCode.NetworkError,
        PurchasesErrorCode.ReceiptAlreadyInUseError,
        PurchasesErrorCode.UnexpectedBackendResponseError,
        PurchasesErrorCode.InvalidAppUserIdError,
        PurchasesErrorCode.OperationAlreadyInProgressError,
        PurchasesErrorCode.UnknownBackendError,
        PurchasesErrorCode.LogOutWithAnonymousUserError,
        PurchasesErrorCode.ConfigurationError,
        PurchasesErrorCode.UnsupportedError,
        PurchasesErrorCode.EmptySubscriberAttributesError,
        PurchasesErrorCode.CustomerInfoError,
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
