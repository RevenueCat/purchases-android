package com.revenuecat.purchases.checkpoints

import android.app.Activity
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Presents a checkpoint-triggered ad. Implemented by whichever ad module/app is present and discovered
 * through [java.util.ServiceLoader], so the core module can auto-present an ad without a compile-time
 * dependency on any ad SDK. POC counterpart to [CheckpointPresenter].
 */
@InternalRevenueCatAPI
public interface AdCheckpointPresenter {

    /**
     * Presents an ad for [adUnitId]. Implementations must call
     * [AdCheckpointPresenterDelegate.onCheckpointAdFinished] with [callId] exactly once, when the ad
     * reaches its terminal state (shown, failed to load, or dismissed).
     */
    public fun present(
        activity: Activity,
        callId: String,
        adUnitId: String,
        delegate: AdCheckpointPresenterDelegate,
    )
}

@InternalRevenueCatAPI
public interface AdCheckpointPresenterDelegate {
    public fun onCheckpointAdFinished(callId: String, adOutcome: CheckpointAdOutcome)
}
