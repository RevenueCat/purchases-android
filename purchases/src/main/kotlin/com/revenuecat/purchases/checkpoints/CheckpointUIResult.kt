package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Terminal disposition of a presented checkpoint experience. Delivered through
 * [CheckpointListener.onCheckpointUIFinished].
 */
@InternalRevenueCatAPI
public abstract class CheckpointUIResult internal constructor() {

    public class Dismissed : CheckpointUIResult() {
        override fun toString(): String = "Dismissed"
    }

    public class Purchased(
        public val customerInfo: CustomerInfo,
    ) : CheckpointUIResult() {
        override fun toString(): String = "Purchased"
    }

    public class Restored(
        public val customerInfo: CustomerInfo,
    ) : CheckpointUIResult() {
        override fun toString(): String = "Restored"
    }

    public class Error(
        public val error: PurchasesError,
    ) : CheckpointUIResult() {
        override fun toString(): String = "Error(error=$error)"
    }
}
