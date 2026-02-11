package com.revenuecat.purchases.customercenter

import dev.drewhamilton.poko.Poko

/**
 * Data associated with a custom action selection in the Customer Center.
 *
 * This class encapsulates information about a custom action that has been triggered
 * in the Customer Center. Custom actions are defined in the configuration and allow
 * applications to handle specialized user flows beyond well-known actions.
 *
 * ## Usage
 *
 * Custom actions are handled through the CustomerCenterListener:
 *
 * val listener = object : CustomerCenterListener {
 *     override fun onCustomActionSelected(actionIdentifier: String, purchaseIdentifier: String?) {
 *         // Handle the custom action
 *         when (actionIdentifier) {
 *             "delete_user" -> deleteUserAccount()
 *             "rate_app" -> showAppStoreRating()
 *             else -> {
 *                 // Handle unknown action
 *             }
 *         }
 *     }
 * }
 */
@Poko
public class CustomActionData(
    /**
     * The unique identifier for the custom action.
     *
     * This identifier is configured in the Customer Center dashboard and allows
     * applications to distinguish between different types of custom actions.
     */
    val actionIdentifier: String,

    /**
     * The product identifier of the purchase being viewed in a detail screen, if any.
     *
     * This provides context about which specific purchase the custom action relates to.
     * It will be `null` if the custom action was triggered from the general management screen
     * rather than from a specific purchase detail screen.
     *
     * - When triggered from a purchase detail screen: Contains the product identifier of that purchase
     * - When triggered from the management screen: Will be `null`
     */
    val purchaseIdentifier: String?,
)
