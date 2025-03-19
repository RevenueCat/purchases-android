package com.revenuecat.purchases.customercenter

import android.net.Uri

/**
 * Interface representing different customer center management options.
 */
interface CustomerCenterManagementOption {
    /**
     * Action to cancel the current operation
     */
    object Cancel : CustomerCenterManagementOption

    /**
     * Action to open a custom URL
     * @property uri The URI to open
     */
    data class CustomUrl(val uri: Uri) : CustomerCenterManagementOption

    /**
     * Action to handle a missing purchase
     */
    object MissingPurchase : CustomerCenterManagementOption
}
