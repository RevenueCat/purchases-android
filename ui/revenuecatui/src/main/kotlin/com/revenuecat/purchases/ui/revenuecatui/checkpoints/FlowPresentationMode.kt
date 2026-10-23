package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * How the SDK presents the flow a checkpoint resolves to, a workflow or the offering's paywall. Every mode is a modal
 * window over the current activity; they differ in how much of the screen the flow takes.
 */
@InviteOnlyCheckpointsAPI
public class FlowPresentationMode private constructor(private val name: String) {

    override fun equals(other: Any?): Boolean = other is FlowPresentationMode && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    public companion object {
        /** The SDK chooses the presentation. Currently, that is [MODAL_SHEET]. */
        @JvmField
        public val DEFAULT: FlowPresentationMode = FlowPresentationMode("DEFAULT")

        /** The flow covers the whole screen. */
        @JvmField
        public val MODAL_FULL_SCREEN: FlowPresentationMode = FlowPresentationMode("MODAL_FULL_SCREEN")

        /**
         * The flow is a modal sheet over the app's content: it slides up to just below the status bar, and swiping it
         * down or tapping outside it closes the flow, like a close action would.
         */
        @JvmField
        public val MODAL_SHEET: FlowPresentationMode = FlowPresentationMode("MODAL_SHEET")
    }
}
