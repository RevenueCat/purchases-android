package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * How the SDK presents the flow a checkpoint resolves to, a workflow or the offering's paywall, over the current
 * activity. Presenters supplied by the app are not affected: they present however they want.
 */
@InviteOnlyCheckpointsAPI
public class CheckpointPresentationMode private constructor(private val name: String) {

    override fun equals(other: Any?): Boolean = other is CheckpointPresentationMode && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    public companion object {
        /** The SDK chooses the presentation. Currently, that is [SHEET]. */
        @JvmField
        public val DEFAULT: CheckpointPresentationMode = CheckpointPresentationMode("DEFAULT")

        /** The flow covers the whole screen. */
        @JvmField
        public val FULL_SCREEN: CheckpointPresentationMode = CheckpointPresentationMode("FULL_SCREEN")

        /**
         * The flow is a modal sheet over the app's content: it slides up to just below the status bar, and swiping it
         * down or tapping outside it dismisses it as if the user had navigated back.
         */
        @JvmField
        public val SHEET: CheckpointPresentationMode = CheckpointPresentationMode("SHEET")
    }
}
