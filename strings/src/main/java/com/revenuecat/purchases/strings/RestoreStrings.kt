package com.revenuecat.purchases.strings

object RestoreStrings {
    const val PURCHASE_HISTORY_EMPTY = "Purchase history is empty."
    const val PURCHASE_HISTORY_RETRIEVED = "Purchase history retrieved %s"
    const val PURCHASE_RESTORED = "Purchase %s restored"
    const val QUERYING_PURCHASE = "Querying %s"
    const val QUERYING_PURCHASE_WITH_HASH = "Purchase of type %s with hash %s"
    const val QUERYING_PURCHASE_WITH_TYPE = "Querying Purchase with %s and type %s"
    const val RESTORING_PURCHASE = "Restoring purchases"
    const val RESTORING_PURCHASE_ERROR = "Error restoring purchase: %s. Error: %s"
    const val SHARING_ACC_RESTORE_FALSE = "allowSharingPlayStoreAccount is set to false and restorePurchases " +
            "has been called. This will 'alias' any app user id's sharing the same receipt. " +
            "Are you sure you want to do this? More info here: https://errors.rev.cat/allowsSharingPlayStoreAccount"
    const val QUERYING_PURCHASE_HISTORY = "Querying purchase history for type %s"
}
