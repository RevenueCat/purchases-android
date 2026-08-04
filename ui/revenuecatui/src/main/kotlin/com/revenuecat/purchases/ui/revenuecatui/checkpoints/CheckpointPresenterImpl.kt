package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Intent
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.checkpoints.CheckpointPresenter
import com.revenuecat.purchases.checkpoints.CheckpointPresenterDelegate
import com.revenuecat.purchases.checkpoints.CheckpointWorkflowPresentation

/**
 * [CheckpointPresenter] implementation instantiated by the core module through [java.util.ServiceLoader]
 * (registered in `META-INF/services`).
 */
@InternalRevenueCatAPI
public class CheckpointPresenterImpl : CheckpointPresenter {

    override fun present(
        activity: Activity,
        callId: String,
        presentation: CheckpointWorkflowPresentation,
        delegate: CheckpointPresenterDelegate,
    ) {
        CheckpointCallStore.store(callId, CheckpointCallStore.Entry(delegate, presentation))
        activity.startActivity(
            Intent(activity, CheckpointWorkflowActivity::class.java)
                .putExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID, callId),
        )
    }
}
