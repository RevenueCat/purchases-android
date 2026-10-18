package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CustomerInfo
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
    /**
     * The customer's information once the checkpoint is passed: synced with what they obtained when the presented
     * flow ended with a purchase or restore, the latest known otherwise.
     */
    public val customer: CustomerInfo,
) {

    /** The entitlement with [identifier] the user obtained during this checkpoint, or null. */
    public fun obtainedEntitlement(identifier: String): ObtainedEntitlement? = flow?.obtainedEntitlement(identifier)

    /** Whether the user obtained the entitlement with [identifier] during this checkpoint. */
    public fun didObtainEntitlement(identifier: String): Boolean = obtainedEntitlement(identifier) != null

    /**
     * The product with [identifier] the user obtained during this checkpoint, or null. [identifier] is the store
     * product identifier: for a Play subscription, the subscription's product identifier without base plan.
     */
    public fun obtainedProduct(identifier: String): ObtainedProduct? = flow?.obtainedProduct(identifier)

    /** Whether the user obtained the product with [identifier] during this checkpoint. */
    public fun didObtainProduct(identifier: String): Boolean = obtainedProduct(identifier) != null
}
