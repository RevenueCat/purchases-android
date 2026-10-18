package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * What kind of failure an error handed to an [ErrorPresenter] is: what failed, relative to the flow the checkpoint
 * resolves to, so the app can word the error for the customer. What the SDK does once the presenter completes is
 * described on [ErrorPresenter.Completion.complete].
 */
@InviteOnlyCheckpointsAPI
public class CheckpointFailureKind private constructor(private val name: String) {

    override fun equals(other: Any?): Boolean = other is CheckpointFailureKind && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    public companion object {
        /** The flow could not be presented, or could not load what it would show. */
        @JvmField
        public val FLOW_COULD_NOT_BE_SHOWN: CheckpointFailureKind = CheckpointFailureKind("FLOW_COULD_NOT_BE_SHOWN")

        /** An action in the flow on screen failed, such as a purchase or a restore. */
        @JvmField
        public val FLOW_ACTION_FAILED: CheckpointFailureKind = CheckpointFailureKind("FLOW_ACTION_FAILED")

        /**
         * The flow ended, and the SDK could not sync what the customer obtained during it. A flow presented
         * through a [PaywallPresenter] is synced afterwards, the app's own or the SDK's paywall for an offering,
         * and so is a workflow in which the app's own purchase logic completed a purchase or restore; a workflow
         * whose purchases RevenueCat completes reports them as they happen and never fails this way.
         */
        @JvmField
        public val FLOW_RESULT_SYNC_FAILED: CheckpointFailureKind = CheckpointFailureKind("FLOW_RESULT_SYNC_FAILED")
    }
}
