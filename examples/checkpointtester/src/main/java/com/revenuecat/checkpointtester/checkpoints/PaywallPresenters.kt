package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPresentationMode
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.paywallPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An app-owned paywall presenter: presenting parks the request in a StateFlow, the app root renders a paywall over
 * everything while one is set, and the paywall reports back through [Request.finish].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
class ParkedPaywallPresenter : PaywallPresenter {

    inner class Request(
        val params: PaywallPresenter.Params,
        private val completion: PaywallPresenter.Completion,
    ) {
        fun finish(result: PaywallPresenter.Completion.Result) {
            completion.complete(result)
            _request.value = null
        }
    }

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    override fun present(params: PaywallPresenter.Params, completion: PaywallPresenter.Completion) {
        _request.value = Request(params, completion)
    }
}

/**
 * The tester's paywall presenters and the [Mode] selected on the main screen, which decides who presents the
 * offering a checkpoint resolves to, plus the [CheckpointPresentationMode] the SDK uses for the flows it presents
 * itself. Every checkpoint call in the tester builds its params through [params] so both selections apply wherever
 * a flow is presented.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
object PaywallPresenters {

    enum class Mode(val label: String, val description: String) {
        Default(
            label = "Default",
            description = "No presenter registered: the SDK presents the offering's paywall, or the default one.",
        ),
        Global(
            label = "Global",
            description = "Purchases.paywallPresenter is set: the app presents every offering with its own paywall.",
        ),
        Local(
            label = "Local",
            description = "Every call passes its own presenter in CheckpointParams, ahead of the registered " +
                "global one, and the app presents the offering with its own paywall.",
        ),
    }

    /** Registered on Purchases in the Global and Local modes. */
    val global = ParkedPaywallPresenter()

    /** Passed in every call's CheckpointParams in the Local mode, so it wins over [global]. */
    val local = ParkedPaywallPresenter()

    private val _mode = MutableStateFlow(Mode.Default)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _presentationMode = MutableStateFlow(CheckpointPresentationMode.DEFAULT)

    /** How flows are presented: by the SDK for workflows and offerings in [Mode.Default], and by the app otherwise. */
    val presentationMode: StateFlow<CheckpointPresentationMode> = _presentationMode.asStateFlow()

    fun select(mode: Mode) {
        _mode.value = mode
        Purchases.sharedInstance.paywallPresenter = if (mode == Mode.Default) null else global
    }

    fun selectPresentationMode(mode: CheckpointPresentationMode) {
        _presentationMode.value = mode
    }

    fun params(block: CheckpointParams.Builder.() -> Unit = {}): CheckpointParams = CheckpointParams {
        block()
        presentationMode(_presentationMode.value)
        if (_mode.value == Mode.Local) paywallPresenter(local)
        ErrorPresenters.apply(this)
    }
}
