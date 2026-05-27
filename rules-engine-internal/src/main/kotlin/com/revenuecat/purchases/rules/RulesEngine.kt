package com.revenuecat.purchases.rules

/**
 * Namespace for the RevenueCat rules engine.
 *
 * This module is an implementation detail of the RevenueCat SDK. It's
 * pulled in by `:purchases` / `:ui:revenuecatui` as an
 * `implementation` dependency on `:rules-engine-internal`, which keeps
 * every declaration here off the SDK's transitive compile classpath —
 * third-party consumers never see these symbols. Mirrors the iOS
 * `RulesEngineInternal` module / `RulesEngine` namespace.
 *
 * Internal for now — will become public once wired into the SDK.
 */
internal object RulesEngine
