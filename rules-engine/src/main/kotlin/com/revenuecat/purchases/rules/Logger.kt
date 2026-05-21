package com.revenuecat.purchases.rules

/**
 * Logging facade for the rules engine.
 *
 * The engine never logs directly; it routes diagnostic warnings through
 * [Rules.logger] so the host SDK can install an adapter that forwards
 * into the same logging pipeline used by the rest of the SDK.
 */
public interface RulesEngineLogger {
    public fun warn(message: String)
}

/**
 * Stop-gap default for [Rules.logger]: writes warnings to stderr via
 * `System.err.println` so they don't get swallowed by release-mode log
 * filters that ignore plain `System.out.println`.
 *
 * Kept module-private on purpose. The host SDK is expected to inject its
 * own adapter at integration time, so external callers never need to
 * reference this implementation.
 */
internal object PrintlnLogger : RulesEngineLogger {
    override fun warn(message: String) {
        System.err.println("[RulesEngine] $message")
    }
}
