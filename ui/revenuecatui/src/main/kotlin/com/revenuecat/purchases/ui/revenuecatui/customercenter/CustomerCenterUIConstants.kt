@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Constants for padding, spacing, sizes, and default colors
 **/
internal object CustomerCenterUIConstants {

    private val PaddingTiny = 4.dp
    private val PaddingSmall = 8.dp
    private val PaddingMedium = 16.dp
    private val PaddingXL = 32.dp

    val ManagementViewTitleTopPadding = 64.dp

    val SettingsRowMainTextSize = 20.sp
    val SettingsRowSupportingTextSize = 14.sp
    const val SettingsRowMainTextAlpha = 1.0f
    const val SettingsRowSupportingTextAlpha = 0.6f

    val SubscriptionViewRowHeight = 60.dp
    val SubscriptionViewHorizontalSpace = PaddingSmall
    val SubscriptionViewIconSize = 24.dp

    val ContentUnavailableViewPaddingHorizontal = 16.dp
    val ContentUnavailableViewPaddingVertical = 20.dp
    val ContentUnavailableViewPaddingText = PaddingTiny
    val ContentUnavailableIconSize = 24.dp

    val ManagementViewHorizontalPadding = PaddingMedium
    val ManagementViewSpacer = PaddingXL
}
