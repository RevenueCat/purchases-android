package com.revenuecat.purchases.codegen.api

import java.io.Serializable

internal data class EntitlementSchema(
    val id: String,
    val lookupKey: String,
    val displayName: String,
) : Serializable

internal data class OfferingSchema(
    val id: String,
    val lookupKey: String,
    val displayName: String,
    val isCurrent: Boolean,
    val packages: List<PackageSchema>,
) : Serializable

internal data class PackageSchema(
    val id: String,
    val lookupKey: String,
    val displayName: String,
) : Serializable

internal data class ProjectSchema(
    val entitlements: List<EntitlementSchema>,
    val offerings: List<OfferingSchema>,
) : Serializable
