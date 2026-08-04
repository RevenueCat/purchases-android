package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import dev.drewhamilton.poko.Poko

/**
 * Everything a [CheckpointPresenter] needs to present the workflow resolved for a checkpoint.
 */
@InternalRevenueCatAPI
@Poko
public class CheckpointWorkflowPresentation internal constructor(
    public val checkpoint: CheckpointInfo,
    public val workflow: PublishedWorkflow,
    public val uiConfig: UiConfig,
    public val offering: Offering?,
)
