package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingCompletion
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The simplest possible app-owned paywall: presenting parks the request in a StateFlow, the app root renders
 * a DummyOfferingPaywallDialog while one is set, and the dialog's buttons report back through the completion.
 */
@OptIn(InternalRevenueCatAPI::class)
object DummyOfferingPaywallPresenter : CheckpointOfferingPresenter {

    class Request(
        val offering: Offering,
        val completion: CheckpointOfferingCompletion,
    )

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    override fun present(offering: Offering, completion: CheckpointOfferingCompletion) {
        _request.value = Request(offering, completion)
    }

    fun clear() {
        _request.value = null
    }
}
