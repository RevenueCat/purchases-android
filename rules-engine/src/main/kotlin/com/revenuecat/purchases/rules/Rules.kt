package com.revenuecat.purchases.rules

/** Namespace for the RevenueCat rules engine. */
@InternalRulesEngineAPI
public object Rules {
    @Volatile
    public var logger: RulesEngineLogger = PrintlnLogger
}
