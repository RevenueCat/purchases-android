package com.revenuecat.purchases.strings

object RestoreStrings {
    const val FIND_QUERYING_PURCHASE = "[QueryPurchases] Querying Purchase with %s and type %s"
    const val PURCHASE_RESTORED = "Purchase %s restored"
    const val QUERYING_PURCHASE = "[QueryPurchases] Querying %s"
    const val QUERYING_PURCHASE_RESULT = "[QueryPurchases] Purchase of type %s with hash %s"
    const val RESTORE_PURCHASE = "Restoring purchases"
    const val RESTORE_PURCHASE_ERROR = "Error restoring purchase: %s. Error: %s"
    const val SHARING_ACC_RESTORE_FALSE = "allowSharingPlayStoreAccount is set to false and restorePurchases " +
            "has been called. This will 'alias' any app user id's sharing the same receipt. " +
            "Are you sure you want to do this? More info here: https://errors.rev.cat/allowsSharingPlayStoreAccount"
}
