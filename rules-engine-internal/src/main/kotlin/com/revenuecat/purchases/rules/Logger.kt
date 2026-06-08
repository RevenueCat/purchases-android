package com.revenuecat.purchases.rules

/** Default log tag for [RulesEngineLogger]. */
public const val RULES_ENGINE_LOG_TAG: String = "[RulesEngine]"

/**
 * Logging facade for the rules engine.
 *
 * Two distinct channels:
 * - [warn] carries engine diagnostics (missing variables, unsupported
 *   operators, type mismatches).
 * - [log] carries pass-through output from the JSON Logic `log` operator,
 *   kept separate so hosts can route it independently (e.g. at debug level).
 *   Defaults to [warn] for back-compat with loggers that don't override it.
 */
public interface RulesEngineLogger {
    public fun warn(message: String, tag: String = RULES_ENGINE_LOG_TAG)

    public fun log(message: String, tag: String = RULES_ENGINE_LOG_TAG) {
        warn(message, tag)
    }
}

/** Default logger for [RulesEngine.logger]. */
internal object PrintLogger : RulesEngineLogger {
    override fun warn(message: String, tag: String) {
        System.err.println("$tag $message")
    }

    override fun log(message: String, tag: String) {
        println("$tag $message")
    }
}
