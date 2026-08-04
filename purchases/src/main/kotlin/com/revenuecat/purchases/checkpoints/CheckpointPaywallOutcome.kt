package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import dev.drewhamilton.poko.Poko

/**
 * Terminal result of a checkpoint-presented paywall. Delivered in [CheckpointResult.PaywallPresented] and
 * through [CheckpointListener.onCheckpointPaywallFinished].
 */
@InternalRevenueCatAPI
public abstract class CheckpointPaywallOutcome internal constructor() {

    public object Dismissed : CheckpointPaywallOutcome() {
        override fun toString(): String = "Dismissed"
    }

    @Poko
    public class Purchased(
        public val customerInfo: CustomerInfo,
    ) : CheckpointPaywallOutcome()

    @Poko
    public class Restored(
        public val customerInfo: CustomerInfo,
    ) : CheckpointPaywallOutcome()

    @Poko
    public class Error(
        public val error: PurchasesError,
    ) : CheckpointPaywallOutcome()
}
