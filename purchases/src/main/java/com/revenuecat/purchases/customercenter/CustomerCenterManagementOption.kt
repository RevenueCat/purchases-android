package com.revenuecat.purchases.customercenter

import android.net.Uri
import dev.drewhamilton.poko.Poko

/**
 * Interface representing different customer center management options.
 */
public interface CustomerCenterManagementOption {
    /**
     * Action to cancel the current operation
     */
    public object Cancel : CustomerCenterManagementOption

    /**
     * Action to open a custom URL
     * @property uri The URI to open
     */
    @Poko
    public class CustomUrl(val uri: Uri) : CustomerCenterManagementOption

    /**
     * Action to handle a missing purchase
     */
    public object MissingPurchase : CustomerCenterManagementOption

    /**
     * Action representing a custom action configured in the Customer Center dashboard.
     * @property actionIdentifier The unique identifier for the custom action
     * @property purchaseIdentifier The optional product identifier of the active purchase
     */
    @Poko
    public class CustomAction(
        val actionIdentifier: String,
        val purchaseIdentifier: String?,
    ) : CustomerCenterManagementOption
}
