package com.revenuecat.purchases.common

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.networking.HTTPResult
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
    BackendInvalidSubscriberAttributesBody(7264),
    BackendProductIDsMalformed(7662);

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

fun HTTPResult.toPurchasesError(): PurchasesError {
    val errorCode = if (body.has("code")) body.get("code") as Int else null
    val errorMessage = if (body.has("message")) body.get("message") as String else ""

    return errorCode?.let { BackendErrorCode.valueOf(it) }?.toPurchasesError(errorMessage)
        ?: PurchasesError(
            PurchasesErrorCode.UnknownBackendError,
            "Backend Code: ${errorCode ?: "N/A"} - $errorMessage"
        )
}

@Suppress("ComplexMethod")
fun BackendErrorCode.toPurchasesErrorCode(): PurchasesErrorCode {
    return when (this) {
        BackendErrorCode.BackendStoreProblem -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendCannotTransferPurchase -> PurchasesErrorCode.ReceiptAlreadyInUseError
        BackendErrorCode.BackendInvalidReceiptToken -> PurchasesErrorCode.InvalidReceiptError
        BackendErrorCode.BackendInvalidPlayStoreCredentials,
        BackendErrorCode.BackendInvalidAuthToken,
        BackendErrorCode.BackendInvalidAPIKey -> PurchasesErrorCode.InvalidCredentialsError
        BackendErrorCode.BackendInvalidPaymentModeOrIntroPriceNotProvided,
        BackendErrorCode.BackendProductIdForGoogleReceiptNotProvided -> PurchasesErrorCode.PurchaseInvalidError
        BackendErrorCode.BackendEmptyAppUserId -> PurchasesErrorCode.InvalidAppUserIdError
        BackendErrorCode.BackendPlayStoreQuotaExceeded -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendPlayStoreInvalidPackageName,
        BackendErrorCode.BackendInvalidPlatform -> PurchasesErrorCode.ConfigurationError
        BackendErrorCode.BackendPlayStoreGenericError -> PurchasesErrorCode.StoreProblemError
        BackendErrorCode.BackendUserIneligibleForPromoOffer -> PurchasesErrorCode.IneligibleError
        BackendErrorCode.BackendInvalidSubscriberAttributes,
        BackendErrorCode.BackendInvalidSubscriberAttributesBody -> PurchasesErrorCode.InvalidSubscriberAttributesError
        BackendErrorCode.BackendInvalidAppStoreSharedSecret,
        BackendErrorCode.BackendInvalidAppleSubscriptionKey,
        BackendErrorCode.BackendBadRequest,
        BackendErrorCode.BackendInternalServerError -> PurchasesErrorCode.UnexpectedBackendResponseError
        BackendErrorCode.BackendProductIDsMalformed -> PurchasesErrorCode.UnsupportedError
    }
}

fun @receiver:BillingClient.BillingResponseCode Int.getBillingResponseCodeName(): String {
    val allPossibleBillingResponseCodes = BillingClient.BillingResponseCode::class.java.declaredFields
    return allPossibleBillingResponseCodes
        .firstOrNull { it.getInt(it) == this }
        ?.name
        ?: "$this"
}

fun Int.billingResponseToPurchasesError(underlyingErrorMessage: String): PurchasesError {
    val errorCode = when (this) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> PurchasesErrorCode.PurchaseNotAllowedError
        BillingClient.BillingResponseCode.ERROR,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> PurchasesErrorCode.StoreProblemError
        BillingClient.BillingResponseCode.OK -> PurchasesErrorCode.UnknownError
        BillingClient.BillingResponseCode.USER_CANCELED -> PurchasesErrorCode.PurchaseCancelledError
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> PurchasesErrorCode.ProductNotAvailableForPurchaseError
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> PurchasesErrorCode.PurchaseInvalidError
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchasesErrorCode.ProductAlreadyPurchasedError
        else -> PurchasesErrorCode.UnknownError
    }
    return PurchasesError(errorCode, underlyingErrorMessage)
}

data class SubscriberAttributeError(
    val keyName: String,
    val message: String
)
