package com.revenuecat.purchases

import android.app.Application

/**
 * This class represents an error
 * @param code Error code
 * @param underlyingErrorMessage Optional error message with a more detailed explanation of the
 * error that originated this.
 */
class PurchasesError {

    val code: PurchasesErrorCode
    val message: String
    val underlyingErrorMessage: String?

    constructor(code: PurchasesErrorCode, underlyingErrorMessage: String? = null, context: Application) {
        this.code = code
        this.underlyingErrorMessage = underlyingErrorMessage
        this.message = context.getString(code.description)
    }

    override fun toString(): String {
        return "PurchasesError(code=$code, underlyingErrorMessage=$underlyingErrorMessage, message='$message')"
    }
}

enum class PurchasesErrorCode(val description: Int) {
    UnknownError(R.string.purchase_was_cancelled),
    PurchaseCancelledError(R.string.purchase_was_cancelled),
    StoreProblemError(R.string.purchase_was_cancelled),
    PurchaseNotAllowedError(R.string.purchase_was_cancelled),
    PurchaseInvalidError(R.string.purchase_was_cancelled),
    ProductNotAvailableForPurchaseError(R.string.purchase_was_cancelled),
    ProductAlreadyPurchasedError(R.string.purchase_was_cancelled),
    ReceiptAlreadyInUseError(R.string.purchase_was_cancelled),
    InvalidReceiptError(R.string.purchase_was_cancelled),
    MissingReceiptFileError(R.string.purchase_was_cancelled),
    NetworkError(R.string.purchase_was_cancelled),
    InvalidCredentialsError(R.string.purchase_was_cancelled),
    UnexpectedBackendResponseError(R.string.purchase_was_cancelled),
    ReceiptInUseByOtherSubscriberError(R.string.purchase_was_cancelled),
    InvalidAppUserIdError(R.string.purchase_was_cancelled),
    OperationAlreadyInProgressError(R.string.purchase_was_cancelled),
    UnknownBackendError(R.string.purchase_was_cancelled),
    InvalidAppleSubscriptionKeyError(R.string.purchase_was_cancelled),
    IneligibleError(R.string.purchase_was_cancelled),
    InsufficientPermissionsError(R.string.purchase_was_cancelled),
    PaymentPendingError(R.string.purchase_was_cancelled),
    InvalidSubscriberAttributesError(R.string.purchase_was_cancelled)
}
