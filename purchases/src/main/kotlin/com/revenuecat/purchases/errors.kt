package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

typealias PurchasesErrorCallback = (PurchasesError) -> Unit

/**
 * This class represents an error
 * @param code Error code
 * @param underlyingErrorMessage Optional error message with a more detailed explanation of the
 * error that originated this.
 */
@Parcelize
class PurchasesError(
    val code: PurchasesErrorCode,
    val underlyingErrorMessage: String? = null,
) : Parcelable {
    // Message explaining the error
    @IgnoredOnParcel
    val message: String = code.description

    override fun toString(): String {
        return "PurchasesError(code=$code, underlyingErrorMessage=$underlyingErrorMessage, message='$message')"
    }
}

@SuppressWarnings("MagicNumber")
enum class PurchasesErrorCode(val code: Int, val description: String) {
    UnknownError(0, "Unknown error."),
    PurchaseCancelledError(1, "Purchase was cancelled."),
    StoreProblemError(2, "There was a problem with the store."),
    PurchaseNotAllowedError(3, "The device or user is not allowed to make the purchase."),
    PurchaseInvalidError(4, "One or more of the arguments provided are invalid."),
    ProductNotAvailableForPurchaseError(5, "The product is not available for purchase."),
    ProductAlreadyPurchasedError(6, "This product is already active for the user."),
    ReceiptAlreadyInUseError(7, "There is already another active subscriber using the same receipt."),
    InvalidReceiptError(8, "The receipt is not valid."),
    MissingReceiptFileError(9, "The receipt is missing."),
    NetworkError(10, "Error performing request."),
    InvalidCredentialsError(11, "There was a credentials issue. Check the underlying error for more details."),
    UnexpectedBackendResponseError(12, "Received unexpected response from the backend."),
    InvalidAppUserIdError(14, "The app user id is not valid."),
    OperationAlreadyInProgressError(15, "The operation is already in progress."),
    UnknownBackendError(16, "There was an unknown backend error."),
    InvalidAppleSubscriptionKeyError(
        17,
        "Apple Subscription Key is invalid or not present. " +
            "In order to provide subscription offers, you must first generate a subscription key. " +
            "Please see https://docs.revenuecat.com/docs/ios-subscription-offers for more info.",
    ),
    IneligibleError(18, "The User is ineligible for that action."),
    InsufficientPermissionsError(19, "App does not have sufficient permissions to make purchases."),
    PaymentPendingError(20, "The payment is pending."),
    InvalidSubscriberAttributesError(21, "One or more of the attributes sent could not be saved."),
    LogOutWithAnonymousUserError(22, "Called logOut but the current user is anonymous."),
    ConfigurationError(23, "There is an issue with your configuration. Check the underlying error for more details."),
    UnsupportedError(
        24,
        "There was a problem with the operation. Looks like we don't support " +
            "that yet. Check the underlying error for more details.",
    ),
    EmptySubscriberAttributesError(25, "A request for subscriber attributes returned none."),
    CustomerInfoError(28, "There was a problem related to the customer info."),
    SignatureVerificationError(
        36,
        "Request failed signature verification. Please see https://rev.cat/trusted-entitlements for more info.",
    ),
}
