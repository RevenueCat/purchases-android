package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.PurchasesErrorCode;

@SuppressWarnings({"unused"})
final class PurchasesErrorAPI {
    static void check(final PurchasesError error) {
        final PurchasesErrorCode code = error.getCode();
        final String message = error.getMessage();
        final String underlyingErrorMessage = error.getUnderlyingErrorMessage();
    }

    static void check(final PurchasesErrorCode code) {
        switch (code) {
            case UnknownError:
            case PurchaseCancelledError:
            case StoreProblemError:
            case PurchaseNotAllowedError:
            case PurchaseInvalidError:
            case ProductNotAvailableForPurchaseError:
            case ProductAlreadyPurchasedError:
            case ReceiptAlreadyInUseError:
            case InvalidReceiptError:
            case MissingReceiptFileError:
            case NetworkError:
            case InvalidCredentialsError:
            case UnexpectedBackendResponseError:
            case InvalidAppUserIdError:
            case OperationAlreadyInProgressError:
            case UnknownBackendError:
            case InvalidAppleSubscriptionKeyError:
            case IneligibleError:
            case InsufficientPermissionsError:
            case PaymentPendingError:
            case InvalidSubscriberAttributesError:
            case LogOutWithAnonymousUserError:
            case ConfigurationError:
            case UnsupportedError:
            case EmptySubscriberAttributesError:
            case CustomerInfoError:
            case SignatureVerificationError:
        }
    }
}
