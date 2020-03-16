package com.revenuecat.purchases

import com.android.billingclient.api.BillingClient
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * This class represents an error
 * @param code Error code
 * @param underlyingErrorMessage Optional error message with a more detailed explanation of the error  that originated this.
 */
class PurchasesError(
    val code: PurchasesErrorCode,
    val underlyingErrorMessage: String? = null
) {
    // Message explaining the error
    val message: String = code.description

    init {
        errorLog("${code.description}${underlyingErrorMessage?.let { " | $it" }}")
    }

    override fun toString(): String {
        return "PurchasesError(code=$code, underlyingErrorMessage=$underlyingErrorMessage, message='$message')"
    }
}

enum class PurchasesErrorCode(val description: String) {
    UnknownError("Unknown error."),
    PurchaseCancelledError("Purchase was cancelled."),
    StoreProblemError("There was a problem with the Play Store."),
    PurchaseNotAllowedError("The device or user is not allowed to make the purchase."),
    PurchaseInvalidError("One or more of the arguments provided are invalid."),
    ProductNotAvailableForPurchaseError("The product is not available for purchase."),
    ProductAlreadyPurchasedError("This product is already active for the user."),
    ReceiptAlreadyInUseError("There is already another active subscriber using the same receipt."),
    InvalidReceiptError("The receipt is not valid."),
    MissingReceiptFileError("The receipt is missing."),
    NetworkError("Error performing request."),
    InvalidCredentialsError("There was a credentials issue. Check the underlying error for more details."),
    UnexpectedBackendResponseError("Received malformed response from the backend."),
    ReceiptInUseByOtherSubscriberError("The receipt is in use by other subscriber."),
    InvalidAppUserIdError("The app user id is not valid."),
    OperationAlreadyInProgressError("The operation is already in progress."),
    UnknownBackendError("There was an unknown backend error."),
    InvalidAppleSubscriptionKeyError("Apple Subscription Key is invalid or not present. In order to provide subscription offers, you must first generate a subscription key. Please see https://docs.revenuecat.com/docs/ios-subscription-offers for more info."),
    IneligibleError("The User is ineligible for that action."),
    InsufficientPermissionsError("App does not have sufficient permissions to make purchases."),
    PaymentPendingError("The payment is pending."),
    InvalidSubscriberAttributesError("One or more of the attributes sent could not be saved.")
}

internal enum class BackendErrorCode(val value: Int) {
    BackendInvalidPlatform(7000),
    BackendStoreProblem(7101),
    BackendCannotTransferPurchase(7102),
    BackendInvalidReceiptToken(7103),
    BackendInvalidAppStoreSharedSecret(7104),
    BackendInvalidPaymentModeOrIntroPriceNotProvided(7105),
    BackendProductIdForGoogleReceiptNotProvided(7106),
    BackendInvalidPlayStoreCredentials(7107),
    BackendEmptyAppUserId(7220),
    BackendInvalidAuthToken(7224),
    BackendInvalidAPIKey(7225),
    BackendPlayStoreQuotaExceeded(7229),
    BackendPlayStoreInvalidPackageName(7230),
    BackendPlayStoreGenericError(7231),
    BackendUserIneligibleForPromoOffer(7232),
    BackendInvalidAppleSubscriptionKey(7234),
    BackendInvalidSubscriberAttributes(7263),
    BackendInvalidSubscriberAttributesBody(7264);

    companion object {
        fun valueOf(backendErrorCode: Int) : BackendErrorCode? {
            return values().firstOrNull { it.value == backendErrorCode }
        }
    }
}

internal fun Exception.toPurchasesError(): PurchasesError {
    if (this is JSONException || this is IOException) {
        return PurchasesError(PurchasesErrorCode.NetworkError, localizedMessage)
    } else if (this is SecurityException) {
        return PurchasesError(PurchasesErrorCode.InsufficientPermissionsError, localizedMessage)
    }
    return PurchasesError(PurchasesErrorCode.UnknownError, localizedMessage)
}

internal fun BackendErrorCode.toPurchasesError(underlyingErrorMessage: String) =
    PurchasesError(this.toPurchasesErrorCode(), underlyingErrorMessage)

internal fun HTTPClient.Result.toPurchasesError(): PurchasesError {
    var errorCode: Int? = null
    var errorMessage = ""
    body?.let { body ->
        errorCode = if (body.has("code")) body.get("code") as Int else null
        errorMessage = if (body.has("message")) body.get("message") as String else ""
    }

    return errorCode?.let { BackendErrorCode.valueOf(it) }?.toPurchasesError(errorMessage)
        ?: PurchasesError(PurchasesErrorCode.UnknownBackendError, "Backend Code: ${errorCode ?: "N/A"} - $errorMessage")
}

internal fun BackendErrorCode.toPurchasesErrorCode(): PurchasesErrorCode {
    return when(this) {
        BackendErrorCode.BackendInvalidPlatform -> PurchasesErrorCode.UnknownError
        BackendErrorCode.BackendStoreProblem -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendCannotTransferPurchase -> PurchasesErrorCode.ReceiptInUseByOtherSubscriberError
        BackendErrorCode.BackendInvalidReceiptToken -> PurchasesErrorCode.InvalidReceiptError
        BackendErrorCode.BackendInvalidAppStoreSharedSecret,
        BackendErrorCode.BackendInvalidPlayStoreCredentials,
        BackendErrorCode.BackendInvalidAuthToken,
        BackendErrorCode.BackendInvalidAPIKey -> PurchasesErrorCode.InvalidCredentialsError
        BackendErrorCode.BackendInvalidPaymentModeOrIntroPriceNotProvided,
        BackendErrorCode.BackendProductIdForGoogleReceiptNotProvided -> PurchasesErrorCode.PurchaseInvalidError
        BackendErrorCode.BackendEmptyAppUserId ->  PurchasesErrorCode.InvalidAppUserIdError
        BackendErrorCode.BackendPlayStoreQuotaExceeded -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendPlayStoreInvalidPackageName -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendPlayStoreGenericError -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendUserIneligibleForPromoOffer -> PurchasesErrorCode.IneligibleError
        BackendErrorCode.BackendInvalidAppleSubscriptionKey -> PurchasesErrorCode.InvalidAppleSubscriptionKeyError
        BackendErrorCode.BackendInvalidSubscriberAttributes,
        BackendErrorCode.BackendInvalidSubscriberAttributesBody -> PurchasesErrorCode.InvalidSubscriberAttributesError
    }
}

internal fun @receiver:BillingClient.BillingResponseCode Int.getBillingResponseCodeName(): String {
    return when (this) {
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
        BillingClient.BillingResponseCode.OK -> "OK"
        BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        BillingClient.BillingResponseCode.ERROR -> "ERROR"
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
        BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "SERVICE_TIMEOUT"
        else -> "$this"
    }
}

internal fun Int.billingResponseToPurchasesError(underlyingErrorMessage: String): PurchasesError {
    log(underlyingErrorMessage)
    val errorCode = when (this) {
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> PurchasesErrorCode.PurchaseNotAllowedError
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> PurchasesErrorCode.StoreProblemError
        BillingClient.BillingResponseCode.OK -> PurchasesErrorCode.UnknownError
        BillingClient.BillingResponseCode.USER_CANCELED -> PurchasesErrorCode.PurchaseCancelledError
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> PurchasesErrorCode.StoreProblemError
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> PurchasesErrorCode.PurchaseNotAllowedError
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> PurchasesErrorCode.ProductNotAvailableForPurchaseError
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> PurchasesErrorCode.PurchaseInvalidError
        BillingClient.BillingResponseCode.ERROR -> PurchasesErrorCode.StoreProblemError
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchasesErrorCode.ProductAlreadyPurchasedError
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> PurchasesErrorCode.PurchaseNotAllowedError
        BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> PurchasesErrorCode.StoreProblemError
        else -> PurchasesErrorCode.UnknownError
    }
    return PurchasesError(errorCode, underlyingErrorMessage)
}

internal data class SubscriberAttributeError(
    val keyName: String,
    val message: String
)