package com.revenuecat.rcttester.ui.offerings

import androidx.compose.runtime.MutableState
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult

data class PaywallActivityCallbacks(
    val onLaunchPaywallActivity: (Offering) -> Unit,
    val onLaunchPaywallActivityIfNeeded: (String, Offering) -> Unit,
    val paywallResultState: MutableState<PaywallResult?>,
)
