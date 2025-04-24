@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Constants for padding, spacing, sizes, and default colors
 **/
internal object CustomerCenterUIConstants {

    private val PaddingSmall = 8.dp
    private val PaddingMedium = 16.dp
    private val PaddingLarge = 24.dp
    private val PaddingXL = 32.dp

    val ManagementViewTitleTopPadding = 64.dp

    val SettingsRowMainTextSize = 20.sp
    val SettingsRowSupportingTextSize = 14.sp
    const val SettingsRowMainTextAlpha = 1.0f
    const val SettingsRowSupportingTextAlpha = 0.6f

    val SubscriptionViewRowHeight = 60.dp
    val SubscriptionViewHorizontalSpace = PaddingSmall
    val SubscriptionViewIconSize = 24.dp

    val ContentUnavailableViewPadding = PaddingMedium
    val ContentUnavailableViewPaddingTopTitle = PaddingSmall
    val ContentUnavailableViewPaddingTopDescription = PaddingLarge
    val ContentUnavailableIconSize = 56.dp

    val ManagementViewHorizontalPadding = PaddingMedium
    val ManagementViewSpacer = PaddingXL
}
