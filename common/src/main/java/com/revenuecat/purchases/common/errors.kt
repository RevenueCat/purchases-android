package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import org.json.JSONException
import java.io.IOException

enum class BackendErrorCode(val value: Int) {
    BackendInvalidPlatform(7000),
    BackendStoreProblem(7101),
    BackendCannotTransferPurchase(7102),
    BackendInvalidReceiptToken(7103),
    BackendInvalidAppStoreSharedSecret(7104),
    BackendInvalidPaymentModeOrIntroPriceNotProvided(7105),
    BackendProductIdForGoogleReceiptNotProvided(7106),
    BackendInvalidPlayStoreCredentials(7107),
    BackendInternalServerError(7110),
    BackendEmptyAppUserId(7220),
    BackendInvalidAuthToken(7224),
    BackendInvalidAPIKey(7225),
    BackendBadRequest(7226),
    BackendPlayStoreQuotaExceeded(7229),
    BackendPlayStoreInvalidPackageName(7230),
    BackendPlayStoreGenericError(7231),
    BackendUserIneligibleForPromoOffer(7232),
    BackendInvalidAppleSubscriptionKey(7234),
    BackendInvalidSubscriberAttributes(7263),
    BackendInvalidSubscriberAttributesBody(7264);

    companion object {
        fun valueOf(backendErrorCode: Int): BackendErrorCode? {
            return values().firstOrNull { it.value == backendErrorCode }
        }
    }
}

fun Exception.toPurchasesError(): PurchasesError {
    return when (this) {
        is JSONException, is IOException -> {
            PurchasesError(PurchasesErrorCode.NetworkError, localizedMessage)
        }
        is SecurityException -> {
            PurchasesError(PurchasesErrorCode.InsufficientPermissionsError, localizedMessage)
        }
        else -> PurchasesError(PurchasesErrorCode.UnknownError, localizedMessage)
    }
}

fun BackendErrorCode.toPurchasesError(underlyingErrorMessage: String) =
    PurchasesError(this.toPurchasesErrorCode(), underlyingErrorMessage)

fun HTTPClient.Result.toPurchasesError(): PurchasesError {
    var errorCode: Int? = null
    var errorMessage = ""
    body?.let { body ->
        errorCode = if (body.has("code")) body.get("code") as Int else null
        errorMessage = if (body.has("message")) body.get("message") as String else ""
    }

    return errorCode?.let { BackendErrorCode.valueOf(it) }?.toPurchasesError(errorMessage)
        ?: PurchasesError(
            PurchasesErrorCode.UnknownBackendError,
            "Backend Code: ${errorCode ?: "N/A"} - $errorMessage"
        )
}

fun BackendErrorCode.toPurchasesErrorCode(): PurchasesErrorCode {
    return when (this) {
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
        BackendErrorCode.BackendEmptyAppUserId -> PurchasesErrorCode.InvalidAppUserIdError
        BackendErrorCode.BackendPlayStoreQuotaExceeded -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendPlayStoreInvalidPackageName -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendPlayStoreGenericError -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendUserIneligibleForPromoOffer -> PurchasesErrorCode.IneligibleError
        BackendErrorCode.BackendInvalidAppleSubscriptionKey -> PurchasesErrorCode.InvalidAppleSubscriptionKeyError
        BackendErrorCode.BackendInvalidSubscriberAttributes,
        BackendErrorCode.BackendInvalidSubscriberAttributesBody -> PurchasesErrorCode.InvalidSubscriberAttributesError
        BackendErrorCode.BackendBadRequest,
        BackendErrorCode.BackendInternalServerError -> PurchasesErrorCode.UnexpectedBackendResponseError
    }
}

fun @receiver:BillingClient.BillingResponseCode Int.getBillingResponseCodeName(): String {
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

fun Int.billingResponseToPurchasesError(underlyingErrorMessage: String): PurchasesError {
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

data class SubscriberAttributeError(
    val keyName: String,
    val message: String
)
