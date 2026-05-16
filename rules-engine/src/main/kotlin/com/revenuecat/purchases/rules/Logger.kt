package com.revenuecat.purchases.rules

/**
 * Module-internal logging facade.
 *
 * Intentionally NOT exposed via the public API in this slice. It is shaped
 * so that a future host-supplied logger can be adapted to the same
 * `RulesEngineLogger` interface without changing any caller.
 *
 * Default behaviour during development is noisy ([PrintlnLogger]); the
 * production default will be revisited once the engine is wired up to the
 * rest of the SDK.
 */
internal interface RulesEngineLogger {
    fun warn(message: String)
}

/**
 * Default logger used by the in-module callers: writes warnings to stderr
 * via `System.err.println` so warnings don't get lost in release-mode log
 * filters that ignore plain `System.out.println`.
 */
internal object PrintlnLogger : RulesEngineLogger {
    override fun warn(message: String) {
        System.err.println("[RulesEngine] $message")
    }
}

/**
 * Static accessor used by the engine internals (`Evaluator`, operators) so
 * the logger does not have to be threaded through every function call.
 *
 * Production callers that want to capture warnings replace [sink] before
 * invoking the engine (and restore it afterwards). Tests do the same via
 * `CapturingLoggerRule`. The default sink is [PrintlnLogger].
 */
internal object RulesEngineLog {
    @Volatile
    var sink: RulesEngineLogger = PrintlnLogger

    fun warn(message: String) {
        sink.warn(message)
    }
}
