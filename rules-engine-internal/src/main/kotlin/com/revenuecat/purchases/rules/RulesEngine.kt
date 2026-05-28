package com.revenuecat.purchases.rules

/** Namespace for the RevenueCat rules engine. */
public object RulesEngine {
    @Volatile
    public var logger: RulesEngineLogger = PrintLogger
}
