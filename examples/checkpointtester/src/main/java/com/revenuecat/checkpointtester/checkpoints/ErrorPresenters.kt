package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.errorPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An app-owned error presenter: presenting parks the request in a StateFlow, the app root renders a dialog over
 * everything while one is set, and the dialog reports how the flow goes on through [Request.finish].
 */
@OptIn(InternalRevenueCatAPI::class)
class ParkedErrorPresenter : ErrorPresenter {

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

/**
 * The tester's error presenters and the [Mode] selected on the main screen, which decides who presents the errors
 * of the flows the SDK presents. [PaywallPresenters.params] applies the selection to every checkpoint call.
 */
@OptIn(InternalRevenueCatAPI::class)
object ErrorPresenters {

    enum class Mode(val label: String, val description: String) {
        Default(
            label = "Default",
            description = "No presenter registered: the SDK shows its own error dialog through a presenter of " +
                "its own.",
        ),
        Global(
            label = "Global",
            description = "Purchases.errorPresenter is set: the app's dialog presents every error of a flow the " +
                "SDK presents.",
        ),
        Local(
            label = "Local",
            description = "Every call passes its own presenter in CheckpointParams, ahead of the registered " +
                "global one.",
        ),
    }

    /** Registered on Purchases in the Global and Local modes. */
    val global = ParkedErrorPresenter()

    /** Passed in every call's CheckpointParams in the Local mode, so it wins over [global]. */
    val local = ParkedErrorPresenter()

    private val _mode = MutableStateFlow(Mode.Default)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    fun select(mode: Mode) {
        _mode.value = mode
        Purchases.sharedInstance.errorPresenter = if (mode == Mode.Default) null else global
    }

    fun apply(builder: CheckpointParams.Builder) {
        if (_mode.value == Mode.Local) builder.errorPresenter(local)
    }
}
