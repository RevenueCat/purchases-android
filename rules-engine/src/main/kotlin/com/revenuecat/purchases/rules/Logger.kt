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
 * Test-only logger that captures messages for assertion. Lives in the
 * production module (rather than under `src/test/`) so non-test callers
 * could reference it from internal helpers without an extra link step.
 */
internal class CapturingLogger : RulesEngineLogger {

    private val captured = mutableListOf<String>()

    val warnings: List<String>
        @Synchronized get() = captured.toList()

    @Synchronized
    override fun warn(message: String) {
        captured += message
    }
}
