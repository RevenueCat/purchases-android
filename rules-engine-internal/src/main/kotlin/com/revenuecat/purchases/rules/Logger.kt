package com.revenuecat.purchases.rules

/** Default log tag for [RulesEngineLogger.warn]. */
public const val RULES_ENGINE_LOG_TAG: String = "[RulesEngine]"

/**
 * Logging facade for the rules engine.
 *
 * Diagnostic warnings are routed through [RulesEngine.logger].
 */
public interface RulesEngineLogger {
    public fun warn(message: String, tag: String = RULES_ENGINE_LOG_TAG)
}

/** Default logger for [RulesEngine.logger]. */
internal object PrintLogger : RulesEngineLogger {
    override fun warn(message: String, tag: String) {
        System.err.println("$tag $message")
    }
}
