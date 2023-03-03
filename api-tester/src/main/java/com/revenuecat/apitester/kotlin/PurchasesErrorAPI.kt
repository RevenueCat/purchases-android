package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

@Suppress("unused", "UNUSED_VARIABLE")
private class PurchasesErrorAPI {
    fun check(error: PurchasesError) {
        val code: PurchasesErrorCode = error.code
        val message: String = error.message
        val underlyingErrorMessage: String? = error.underlyingErrorMessage
    }

    fun check(code: PurchasesErrorCode) {
        when (code) {
            PurchasesErrorCode.UnknownError,
            PurchasesErrorCode.PurchaseCancelledError,
            PurchasesErrorCode.StoreProblemError,
            PurchasesErrorCode.PurchaseNotAllowedError,
            PurchasesErrorCode.PurchaseInvalidError,
            PurchasesErrorCode.ProductNotAvailableForPurchaseError,
            PurchasesErrorCode.ProductAlreadyPurchasedError,
            PurchasesErrorCode.ReceiptAlreadyInUseError,
            PurchasesErrorCode.InvalidReceiptError,
            PurchasesErrorCode.MissingReceiptFileError,
            PurchasesErrorCode.NetworkError,
            PurchasesErrorCode.InvalidCredentialsError,
            PurchasesErrorCode.UnexpectedBackendResponseError,
            PurchasesErrorCode.InvalidAppUserIdError,
            PurchasesErrorCode.OperationAlreadyInProgressError,
            PurchasesErrorCode.UnknownBackendError,
            PurchasesErrorCode.InvalidAppleSubscriptionKeyError,
            PurchasesErrorCode.IneligibleError,
            PurchasesErrorCode.InsufficientPermissionsError,
            PurchasesErrorCode.PaymentPendingError,
            PurchasesErrorCode.InvalidSubscriberAttributesError,
            PurchasesErrorCode.LogOutWithAnonymousUserError,
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.UnsupportedError,
            PurchasesErrorCode.EmptySubscriberAttributesError,
            PurchasesErrorCode.CustomerInfoError,
            PurchasesErrorCode.SignatureVerificationError
            -> {}
        }.exhaustive
    }
}
