package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import dev.drewhamilton.poko.Poko

/**
 * Everything a presenter needs to present the workflow resolved for a checkpoint.
 *
 * [offering]/[uiConfig] are paywall-specific and only set when [adUnitId] is null; [adUnitId] is
 * ad-specific and only set when [offering]/[uiConfig] are null.
 */
@InternalRevenueCatAPI
@Poko
public class CheckpointWorkflowPresentation internal constructor(
    public val checkpoint: CheckpointInfo,
    public val workflow: PublishedWorkflow,
    public val uiConfig: UiConfig? = null,
    public val offering: Offering? = null,
    public val adUnitId: String? = null,
)
