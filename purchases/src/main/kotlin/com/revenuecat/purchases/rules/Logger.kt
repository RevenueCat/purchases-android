package com.revenuecat.purchases.rules

/**
 * Logging facade for the rules engine.
 */
internal interface RulesEngineLogger {
    /**
     * Carries engine diagnostics for conditions the evaluator recovers from,
     * such as ignored extra operator arguments.
     */
    fun warn(message: String)

    /**
     * Carries pass-through output from the JSON Logic `log` operator.
     */
    fun log(message: String)
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
