package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * When, relative to the flow a checkpoint resolves to, an error handed to an [ErrorPresenter] occurred. The stage
 * decides what the SDK does once the presenter completes, and lets the app word the error for the customer.
 */
@InviteOnlyCheckpointsAPI
public class ErrorPresentationStage private constructor(private val name: String) {

    override fun equals(other: Any?): Boolean = other is ErrorPresentationStage && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    public companion object {
        /**
         * The flow could not be presented, or could not load what it would show. The customer saw nothing of it.
         * Completing takes them back to where they were in the app before the checkpoint.
         */
        @JvmField
        public val BEFORE_FLOW_PRESENTATION: ErrorPresentationStage = ErrorPresentationStage("BEFORE_FLOW_PRESENTATION")

        /**
         * The flow is on screen and something in it failed, such as a purchase or a restore. Completing dismisses
         * the error and resumes the flow where it was, so the customer can try again or leave.
         */
        @JvmField
        public val DURING_FLOW_PRESENTATION: ErrorPresentationStage = ErrorPresentationStage("DURING_FLOW_PRESENTATION")

        /**
         * The flow ended, and the SDK could not sync what the customer obtained during it. Completing lets them go
         * on: the checkpoint is passed and its callback receives an empty result.
         */
        @JvmField
        public val AFTER_FLOW_PRESENTATION: ErrorPresentationStage = ErrorPresentationStage("AFTER_FLOW_PRESENTATION")
    }
}
