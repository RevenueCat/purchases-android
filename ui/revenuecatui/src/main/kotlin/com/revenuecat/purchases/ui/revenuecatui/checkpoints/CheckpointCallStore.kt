package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.checkpoints.CheckpointPresenterDelegate
import com.revenuecat.purchases.checkpoints.CheckpointWorkflowPresentation

/**
 * Carries a checkpoint call's non-Parcelable payload across the Intent boundary into
 * [CheckpointWorkflowActivity], keyed by callId. Entries are removed when the terminal result is reported, so
 * a recreated activity (e.g. rotation) can still read its payload.
 */
internal object CheckpointCallStore {

    class Entry(
        val delegate: CheckpointPresenterDelegate,
        val presentation: CheckpointWorkflowPresentation,
    )

    private val entriesByCallId = mutableMapOf<String, Entry>()

    @Synchronized
    fun store(callId: String, entry: Entry) {
        entriesByCallId[callId] = entry
    }

    @Synchronized
    fun get(callId: String): Entry? = entriesByCallId[callId]

    @Synchronized
    fun remove(callId: String): Entry? = entriesByCallId.remove(callId)

    @Synchronized
    fun clear() {
        entriesByCallId.clear()
    }
}
