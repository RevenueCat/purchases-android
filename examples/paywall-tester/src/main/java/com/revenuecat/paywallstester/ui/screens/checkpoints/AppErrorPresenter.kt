package com.revenuecat.paywallstester.ui.screens.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Parks each error the SDK asks this app to present in [request] so the screen can draw its own dialog; the dialog
 * reports how the flow goes on through [Request.finish].
 */
@OptIn(InternalRevenueCatAPI::class)
class AppErrorPresenter : ErrorPresenter {

    inner class Request(
        val params: ErrorPresenter.Params,
        private val completion: ErrorPresenter.Completion,
    ) {
        fun finish(result: ErrorPresenter.Completion.Result) {
            completion.complete(result)
            _request.value = null
        }
    }

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
        _request.value = Request(params, completion)
    }
}
