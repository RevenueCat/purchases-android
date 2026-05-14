package com.revenuecat.purchases.rules

/**
 * Public namespace for the RevenueCat rules engine.
 *
 * This module is an implementation detail of the RevenueCat SDK. Every
 * declaration here is gated by [InternalRulesEngineAPI] so it can be
 * consumed by `:purchases` / `:ui:revenuecatui` (and hybrid bridges)
 * across module boundaries without becoming part of the SDK's public
 * API. Annotate every new public declaration in this module with
 * [InternalRulesEngineAPI].
 */
@InternalRulesEngineAPI
public object RulesEngine

public fun leakedPublicApi(): Int = 0
