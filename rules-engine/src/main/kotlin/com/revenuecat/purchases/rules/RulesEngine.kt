package com.revenuecat.purchases.rules

/**
 * Public namespace for the RevenueCat rules engine.
 *
 * This module is an implementation detail of the RevenueCat SDK. It's
 * pulled in by `:purchases` / `:ui:revenuecatui` as an
 * `implementation` dependency, which keeps every declaration here off
 * the SDK's transitive compile classpath — third-party consumers
 * never see these symbols. Mirrors the iOS `Rules` namespace.
 */
public object RulesEngine
