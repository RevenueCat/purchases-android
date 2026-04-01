package com.revenuecat.purchases.codegen.api

import java.io.Serializable

internal data class EntitlementSchema(
    val id: String,
    val lookupKey: String,
    val displayName: String,
) : Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class OfferingSchema(
    val id: String,
    val lookupKey: String,
    val displayName: String,
    val isCurrent: Boolean,
    val packages: List<PackageSchema>,
) : Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class PackageSchema(
    val id: String,
    val lookupKey: String,
    val displayName: String,
) : Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ProjectSchema(
    val entitlements: List<EntitlementSchema>,
    val offerings: List<OfferingSchema>,
) : Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
