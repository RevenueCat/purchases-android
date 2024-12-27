@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI

/**
 * Composable offering a full screen Customer Center UI configured from the RevenueCat dashboard.
 */
// CustomerCenter WIP: Make public when ready
@JvmSynthetic
@Composable
@ExperimentalPreviewRevenueCatUIPurchasesAPI
@SuppressWarnings("PreviewPublic")
internal fun CustomerCenter(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    InternalCustomerCenter(modifier, onDismiss)
}
