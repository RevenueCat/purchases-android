package com.revenuecat.purchases.rules

/** Namespace for the RevenueCat rules engine. */
public object RulesEngine {
    @Volatile
    private var _logger: RulesEngineLogger = PrintLogger

    internal val logger: RulesEngineLogger
        get() = _logger

    @Synchronized
    public fun setLogger(logger: RulesEngineLogger) {
        _logger = logger
    }
}
