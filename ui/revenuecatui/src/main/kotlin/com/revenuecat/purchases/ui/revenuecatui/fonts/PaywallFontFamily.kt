package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import kotlinx.parcelize.Parcelize

/**
 * Represents a font family. You can add one ore more [PaywallFont] with different weights and font styles.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
@Parcelize
data class PaywallFontFamily(val fonts: List<PaywallFont>) : Parcelable
