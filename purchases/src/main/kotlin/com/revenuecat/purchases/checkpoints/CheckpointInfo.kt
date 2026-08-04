package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko

@InternalRevenueCatAPI
@Poko
public class CheckpointInfo internal constructor(
    public val identifier: String,
    public val params: CheckpointParams,
)
