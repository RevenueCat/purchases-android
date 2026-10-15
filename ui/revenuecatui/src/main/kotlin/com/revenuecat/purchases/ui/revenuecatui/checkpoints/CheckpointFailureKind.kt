package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * What kind of failure an error handed to an [ErrorPresenter] is: what failed, relative to the flow the checkpoint
 * resolves to. The kind decides what the SDK does once the presenter completes, and lets the app word the error
 * for the customer.
 */
@InviteOnlyCheckpointsAPI
public class CheckpointFailureKind private constructor(private val name: String) {

    override fun equals(other: Any?): Boolean = other is CheckpointFailureKind && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    public companion object {
        /**
         * The flow could not be presented, or could not load what it would show. The customer saw nothing of it.
         * Completing takes them back to where they were in the app before the checkpoint.
         */
        @JvmField
        public val FLOW_COULD_NOT_BE_SHOWN: CheckpointFailureKind = CheckpointFailureKind("FLOW_COULD_NOT_BE_SHOWN")

        /**
         * An action in the flow on screen failed, such as a purchase or a restore. Completing dismisses the error
         * and resumes the flow where it was, so the customer can try again or leave.
         */
        @JvmField
        public val FLOW_ACTION_FAILED: CheckpointFailureKind = CheckpointFailureKind("FLOW_ACTION_FAILED")

        /**
         * The flow ended, and the SDK could not sync what the customer obtained during it. Only a flow presented
         * through a [PaywallPresenter] is synced afterwards, the app's own or the SDK's paywall for an offering; a
         * workflow reports its purchases and restores as they happen and never fails this way. Completing lets the
         * customer go on: the checkpoint is passed and its callback receives an empty result.
         */
        @JvmField
        public val FLOW_RESULT_SYNC_FAILED: CheckpointFailureKind = CheckpointFailureKind("FLOW_RESULT_SYNC_FAILED")
    }
}
