package com.revenuecat.paywallstester.ui.screens.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Parks each presentation request in [request] so the screen can draw the app's own paywall; the paywall reports
 * how the user left it through [Request.finish].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
class AppPaywallPresenter(
    private val onFinished: (PaywallPresenter.Completion.Result) -> Unit,
) : PaywallPresenter {

    inner class Request(
        val params: PaywallPresenter.Params,
        private val completion: PaywallPresenter.Completion,
    ) {
        fun finish(result: PaywallPresenter.Completion.Result) {
            completion.complete(result)
            _request.value = null
            onFinished(result)
        }
    }

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    override fun present(params: PaywallPresenter.Params, completion: PaywallPresenter.Completion) {
        _request.value = Request(params, completion)
    }
}
