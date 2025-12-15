package com.android.vending.billing;

/**
 * Inspired by [GmsCore](https://github.com/microg/GmsCore/blob/cb9be8f682d7649dae23bcc1619f8c5717de5b9d/vending-app/src/main/aidl/com/android/vending/billing/IInAppBillingService.aidl)
 */
interface IInAppBillingService {
    /**
     * Returns the most recent purchase made by the user for each SKU, even if that purchase is
     * expired, canceled, or consumed.
     * @param apiVersion billing API version that the app is using, must be 6 or later
     * @param packageName package name of the calling app
     * @param type of the in-app items being requested ("inapp" for one-time purchases
     *        and "subs" for subscriptions)
     * @param continuationToken to be set as null for the first call, if the number of owned
     *        skus is too large, a continuationToken is returned in the response bundle.
     *        This method can be called again with the continuation token to get the next set of
     *        owned skus.
     * @param extraParams a Bundle with the following optional keys:
     *        "playBillingLibraryVersion" - String
     *        "enablePendingPurchases" - Boolean
     * @return Bundle containing the following key-value pairs
     *         "RESPONSE_CODE" with int value: RESULT_OK(0) if success,
     *         {@link IabHelper#BILLING_RESPONSE_RESULT_*} response codes on failures.
     *         "DEBUG_MESSAGE" - String
     *         "INAPP_PURCHASE_ITEM_LIST" - ArrayList<String> containing the list of SKUs
     *         "INAPP_PURCHASE_DATA_LIST" - ArrayList<String> containing the purchase information
     *         "INAPP_DATA_SIGNATURE_LIST"- ArrayList<String> containing the signatures
     *                                      of the purchase information
     *         "INAPP_CONTINUATION_TOKEN" - String containing a continuation token for the
     *                                      next set of in-app purchases. Only set if the
     *                                      user has more owned skus than the current list.
     */
    Bundle getPurchaseHistory(int apiVersion, String packageName, String type,
            String continuationToken, in Bundle extraParams) = 8;
}
