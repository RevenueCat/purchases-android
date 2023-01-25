package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

/**
 * Whether debug logs are enabled
 */
val LogLevel.debugLogsEnabled: Boolean
    get() = this <= LogLevel.DEBUG

fun LogLevel.Companion.debugLogsEnabled(enabled: Boolean): LogLevel {
    return if (enabled) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
}

fun verboseLog(message: String) {
    logIfEnabled(LogLevel.VERBOSE, currentLogHandler::v, message)
}

fun debugLog(message: String) {
    logIfEnabled(LogLevel.DEBUG, currentLogHandler::d, message)
}

fun infoLog(message: String) {
    logIfEnabled(LogLevel.INFO, currentLogHandler::i, message)
}

fun warnLog(message: String) {
    logIfEnabled(LogLevel.WARN, currentLogHandler::w, message)
}

fun errorLog(message: String, throwable: Throwable? = null) {
    currentLogHandler.e("$PURCHASES_LOG_TAG - ${LogLevel.ERROR.name}", message, throwable)
}

private fun logIfEnabled(level: LogLevel, logger: (tag: String, message: String) -> Unit, message: String) {
    if (Config.logLevel <= level) {
        logger("$PURCHASES_LOG_TAG - ${level.name}", message)
    }
}

private const val PURCHASES_LOG_TAG: String = "[Purchases]"

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
