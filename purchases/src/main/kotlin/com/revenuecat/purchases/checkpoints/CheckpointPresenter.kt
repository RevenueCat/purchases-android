package com.revenuecat.purchases.checkpoints

import android.app.Activity
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Presents checkpoint workflows. Implemented by the RevenueCat UI module and discovered through
 * [java.util.ServiceLoader], so the core module can auto-present UI without a compile-time dependency.
 */
@InternalRevenueCatAPI
public interface CheckpointPresenter {

    /**
     * Presents the workflow in [presentation]. Implementations must call
     * [CheckpointPresenterDelegate.onCheckpointPaywallFinished] with [callId] exactly once, when the
     * presented workflow reaches its terminal state.
     */
    public fun present(
        activity: Activity,
        callId: String,
        presentation: CheckpointWorkflowPresentation,
        delegate: CheckpointPresenterDelegate,
    )
}

@InternalRevenueCatAPI
public interface CheckpointPresenterDelegate {
    public fun onCheckpointPaywallFinished(callId: String, paywallOutcome: CheckpointPaywallOutcome)
}
