package com.revenuecat.purchases.rules

/**
 * Logging facade for the rules engine.
 */
public interface RulesEngineLogger {
    /**
     * Carries engine diagnostics (missing variables, unsupported
     * operators, type mismatches).
     */
    public fun warn(message: String)

    /**
     * Carries pass-through output from the JSON Logic `log` operator.
     */
    public fun log(message: String)
}

/** Default logger for [RulesEngine.logger]. */
internal object PrintLogger : RulesEngineLogger {
    override fun warn(message: String) {
        System.err.println(message)
    }

    override fun log(message: String) {
        println(message)
    }
}
