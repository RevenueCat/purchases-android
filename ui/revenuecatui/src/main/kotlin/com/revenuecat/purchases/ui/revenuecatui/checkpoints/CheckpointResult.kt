package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import dev.drewhamilton.poko.Poko

/**
 * The result of passing a checkpoint, delivered to [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint]'s
 * callback.
 */
@InviteOnlyCheckpointsAPI
@Poko
public class CheckpointResult internal constructor(
    /**
     * What the user obtained while going through the flow the checkpoint presented. Null when there was nothing to
     * present: the checkpoint is not configured, or no rule matched.
     */
    public val flow: FlowResult?,
)
