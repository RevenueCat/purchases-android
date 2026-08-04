package com.revenuecat.purchases.checkpoints

import android.app.Activity
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Presents checkpoint experiences. Implemented by the RevenueCat UI module and discovered through
 * [java.util.ServiceLoader], so the core module can auto-present UI without a compile-time dependency.
 */
@InternalRevenueCatAPI
public interface CheckpointPresenter {

    /**
     * Presents the experience for [checkpoint]. Implementations must call
     * [CheckpointPresenterDelegate.onCheckpointUIFinished] with [callId] exactly once, when the
     * experience reaches its terminal state.
     */
    public fun present(
        activity: Activity,
        callId: String,
        checkpoint: CheckpointInfo,
        delegate: CheckpointPresenterDelegate,
    )
}

@InternalRevenueCatAPI
public interface CheckpointPresenterDelegate {
    public fun onCheckpointUIFinished(callId: String, uiResult: CheckpointUIResult)
}
