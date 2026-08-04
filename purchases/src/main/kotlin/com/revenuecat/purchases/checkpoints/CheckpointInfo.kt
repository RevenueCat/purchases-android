package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

@InternalRevenueCatAPI
public class CheckpointInfo internal constructor(
    public val identifier: String,
    public val params: CheckpointParams,
) {
    override fun toString(): String = "CheckpointInfo(identifier='$identifier', params=$params)"
}
