package com.revenuecat.purchases.rules

/**
 * Module-level namespace for the RevenueCat rules engine.
 *
 * This module is an implementation detail of the RevenueCat SDK. Every
 * declaration in this package is gated by [InternalRulesEngineAPI] so it
 * can be consumed by `:purchases` / `:ui:revenuecatui` (and hybrid
 * bridges) across module boundaries without becoming part of the SDK's
 * public API. Annotate every new public declaration in this module with
 * [InternalRulesEngineAPI].
 *
 * The host SDK installs a [logger] adapter once at integration time so
 * the engine's diagnostic warnings flow through the same logging
 * pipeline the rest of the SDK uses. Until that hookup happens the
 * default falls through to a stderr `PrintlnLogger` for development
 * visibility.
 *
 * Mirrors the `Rules` namespace in `RulesEngine` on iOS so the two
 * platforms keep call-site shape (`Rules.logger = ...`) in lockstep.
 */
@InternalRulesEngineAPI
public object Rules {
    @Volatile
    public var logger: RulesEngineLogger = PrintlnLogger
}
