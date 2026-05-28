package com.revenuecat.purchases.rules

/**
 * Logging facade for the rules engine.
 *
 * Diagnostic warnings are routed through [RulesEngine.logger].
 */
public interface RulesEngineLogger {
    public fun warn(message: String)
}

/** Default logger for [RulesEngine.logger]. */
internal object PrintLogger : RulesEngineLogger {
    override fun warn(message: String) {
        System.err.println("[RulesEngine] $message")
    }
}
