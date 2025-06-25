package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonStyle

/**
 * Utility object for categorizing and filtering customer center help paths
 */
internal object PathUtils {

    /**
     * Determines if a path is a general action that should be shown regardless of specific purchase context
     */
    fun isGeneralPath(path: CustomerCenterConfigData.HelpPath): Boolean {
        return when (path.type) {
            CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL,
            CustomerCenterConfigData.HelpPath.PathType.UNKNOWN,
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
    fun isSubscriptionSpecificPath(path: CustomerCenterConfigData.HelpPath): Boolean {
        return when (path.type) {
            CustomerCenterConfigData.HelpPath.PathType.CANCEL,
            CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST,
            CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS,
            -> true
            CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL,
            CustomerCenterConfigData.HelpPath.PathType.UNKNOWN,
            -> false
        }
    }

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
}
