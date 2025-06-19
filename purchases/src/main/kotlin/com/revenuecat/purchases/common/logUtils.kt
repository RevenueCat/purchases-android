package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

/**
 * Whether debug logs are enabled
 */
internal val LogLevel.debugLogsEnabled: Boolean
    get() = this <= LogLevel.DEBUG

internal fun LogLevel.Companion.debugLogsEnabled(enabled: Boolean): LogLevel {
    return if (enabled) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
}

internal fun verboseLog(messageBuilder: () -> String) {
    logIfEnabled(LogLevel.VERBOSE, currentLogHandler::v, messageBuilder)
}

internal fun debugLog(messageBuilder: () -> String) {
    logIfEnabled(LogLevel.DEBUG, currentLogHandler::d, messageBuilder)
}

internal fun infoLog(messageBuilder: () -> String) {
    logIfEnabled(LogLevel.INFO, currentLogHandler::i, messageBuilder)
}

internal fun warnLog(messageBuilder: () -> String) {
    logIfEnabled(LogLevel.WARN, currentLogHandler::w, messageBuilder)
}

internal fun errorLog(throwable: Throwable? = null, messageBuilder: () -> String) {
    currentLogHandler.e("$PURCHASES_LOG_TAG - ${LogLevel.ERROR.name}", messageBuilder(), throwable)
}

internal fun errorLog(message: String, throwable: Throwable? = null) {
    currentLogHandler.e("$PURCHASES_LOG_TAG - ${LogLevel.ERROR.name}", message, throwable)
}

private fun logIfEnabled(
    level: LogLevel,
    logger: (tag: String, message: String) -> Unit,
    messageBuilder: () -> String,
) {
    if (Config.logLevel <= level) {
        logger("$PURCHASES_LOG_TAG - ${level.name}", messageBuilder())
    }
}

private const val PURCHASES_LOG_TAG: String = "[Purchases]"

internal fun errorLog(error: PurchasesError) {
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
        PurchasesErrorCode.SignatureVerificationError,
        PurchasesErrorCode.InvalidSubscriberAttributesError,
        -> log(LogIntent.RC_ERROR) { error.toString() }

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
        PurchasesErrorCode.InvalidCredentialsError,
        -> log(LogIntent.GOOGLE_ERROR) { error.toString() }
    }
}
