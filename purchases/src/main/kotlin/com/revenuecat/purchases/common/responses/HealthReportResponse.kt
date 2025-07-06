package com.revenuecat.purchases.common.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Health check status enum for individual health checks
 */
@Serializable
internal enum class HealthCheckStatus {
    @SerialName("passed")
    PASSED,
    @SerialName("failed")
    FAILED,
    @SerialName("warning")
    WARNING,
    @SerialName("unknown")
    UNKNOWN
}

/**
 * Health check type enum for different types of health checks
 */
@Serializable
internal enum class HealthCheckType {
    @SerialName("api_key")
    API_KEY,
    @SerialName("bundle_id")
    BUNDLE_ID,
    @SerialName("products")
    PRODUCTS,
    @SerialName("offerings")
    OFFERINGS,
    @SerialName("offerings_products")
    OFFERINGS_PRODUCTS
}

/**
 * Details for a health check, containing type-specific information
 */
@Serializable
internal data class HealthCheckDetails(
    @SerialName("app_bundle_id")
    val appBundleId: String? = null,
    @SerialName("sdk_bundle_id")
    val sdkBundleId: String? = null,
    @SerialName("products")
    val products: List<ProductHealthCheckDetails>? = null,
    @SerialName("offerings")
    val offerings: List<OfferingHealthCheckDetails>? = null
)

/**
 * Product-specific health check details
 */
@Serializable
internal data class ProductHealthCheckDetails(
    @SerialName("product_id")
    val productId: String,
    @SerialName("status")
    val status: HealthCheckStatus,
    @SerialName("name")
    val name: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("price")
    val price: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("subscription_period")
    val subscriptionPeriod: String? = null,
    @SerialName("trial_period")
    val trialPeriod: String? = null,
    @SerialName("intro_period")
    val introPeriod: String? = null,
    @SerialName("intro_price")
    val introPrice: String? = null
)

/**
 * Offering-specific health check details
 */
@Serializable
internal data class OfferingHealthCheckDetails(
    @SerialName("offering_id")
    val offeringId: String,
    @SerialName("status")
    val status: HealthCheckStatus,
    @SerialName("packages")
    val packages: List<PackageHealthCheckDetails>? = null
)

/**
 * Package-specific health check details
 */
@Serializable
internal data class PackageHealthCheckDetails(
    @SerialName("package_id")
    val packageId: String,
    @SerialName("status")
    val status: HealthCheckStatus,
    @SerialName("product_id")
    val productId: String? = null
)

/**
 * Individual health check result
 */
@Serializable
internal data class HealthCheck(
    @SerialName("name")
    val name: HealthCheckType,
    @SerialName("status")
    val status: HealthCheckStatus,
    @SerialName("details")
    val details: HealthCheckDetails? = null
)

/**
 * Main health report response from the backend
 */
@Serializable
internal data class HealthReport(
    @SerialName("status")
    val status: HealthCheckStatus,
    @SerialName("project_id")
    val projectId: String? = null,
    @SerialName("app_id")
    val appId: String? = null,
    @SerialName("checks")
    val checks: List<HealthCheck>
)