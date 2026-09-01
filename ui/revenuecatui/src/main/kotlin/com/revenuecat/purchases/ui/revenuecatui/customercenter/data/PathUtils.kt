package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonStyle

/**
 * Utility object for categorizing and filtering customer center help paths
 */
internal object PathUtils {

    /**
     * Filters paths to only include general actions
     */
    fun filterGeneralPaths(paths: List<CustomerCenterConfigData.HelpPath>): List<CustomerCenterConfigData.HelpPath> {
        return paths.filter { isGeneralPath(it) }
    }

    /**
     * Filters paths to only include subscription-specific actions
     */
    fun filterSubscriptionSpecificPaths(
        paths: List<CustomerCenterConfigData.HelpPath>,
    ): List<CustomerCenterConfigData.HelpPath> {
        return paths.filter { isSubscriptionSpecificPath(it) }
    }

    /**
     * Determines the button style for a path based on whether it's general or subscription-specific
     */
    fun getButtonStyleForPath(path: CustomerCenterConfigData.HelpPath): SettingsButtonStyle {
        return if (isSubscriptionSpecificPath(path)) {
            SettingsButtonStyle.FILLED
        } else {
            SettingsButtonStyle.OUTLINED
        }
    }

    /**
     * Sorts paths so that filled buttons (subscription-specific) appear before outlined buttons (general)
     */
    fun sortPathsByButtonPriority(
        paths: List<CustomerCenterConfigData.HelpPath>,
    ): List<CustomerCenterConfigData.HelpPath> {
        return paths.sortedBy { path ->
            when (getButtonStyleForPath(path)) {
                SettingsButtonStyle.FILLED -> 0 // Primary actions first
                SettingsButtonStyle.OUTLINED -> 1 // Secondary actions second
            }
        }
    }

    /**

     * Determines if a path is a general action that should be shown regardless of specific purchase context
     */
    private fun isGeneralPath(path: CustomerCenterConfigData.HelpPath): Boolean {
        return when (path.type) {
            CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL,
            CustomerCenterConfigData.HelpPath.PathType.UNKNOWN,
            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION,
            -> true
            CustomerCenterConfigData.HelpPath.PathType.CANCEL,
            CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST,
            CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS,
            -> false
        }
    }

    /**
     * Determines if a path is a subscription-specific action
     */
    private fun isSubscriptionSpecificPath(path: CustomerCenterConfigData.HelpPath): Boolean {
        return when (path.type) {
            CustomerCenterConfigData.HelpPath.PathType.CANCEL,
            CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST,
            CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS,
            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION,
            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL,
            -> true
            CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
            CustomerCenterConfigData.HelpPath.PathType.UNKNOWN,
            -> false
        }
    }
}
