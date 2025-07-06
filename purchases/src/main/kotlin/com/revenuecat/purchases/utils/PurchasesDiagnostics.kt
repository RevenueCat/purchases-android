package com.revenuecat.purchases.utils

import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesOrchestrator
import com.revenuecat.purchases.interfaces.ReceiveHealthReportCallback

/**
 * Diagnostics utility for the RevenueCat SDK.
 * This class provides methods to check the health of the SDK configuration.
 * 
 * **Note**: This class is intended solely for debugging configuration issues with the SDK implementation.
 * It should not be invoked in production builds.
 */
class PurchasesDiagnostics internal constructor(
    private val purchasesOrchestrator: PurchasesOrchestrator
) {

    companion object {
        /**
         * Default instance of [PurchasesDiagnostics].
         * **Note**: you must call [com.revenuecat.purchases.Purchases.configure] before using this.
         */
        @JvmStatic
        val default: PurchasesDiagnostics
            get() = PurchasesDiagnostics(Purchases.sharedInstance.purchasesOrchestrator)
    }

    /**
     * Performs a full SDK configuration health check and returns its status.
     * 
     * **Important**: This method is intended solely for debugging configuration issues with the SDK implementation.
     * It should not be invoked in production builds.
     * 
     * @param callback The callback to receive the health report result
     */
    fun healthReport(callback: ReceiveHealthReportCallback) {
        if (!BuildConfig.DEBUG) {
            callback.onError(
                PurchasesError(
                    PurchasesErrorCode.ConfigurationError,
                    "PurchasesDiagnostics.healthReport() is only available in debug builds"
                )
            )
            return
        }
        
        // TODO: Implement health report logic
        purchasesOrchestrator.getHealthReport(callback)
    }
}

/**
 * A report that encapsulates the result of the SDK configuration health check.
 * Use this to programmatically inspect the SDK's health status after calling [PurchasesDiagnostics.healthReport].
 */
data class SDKHealthReport(
    /**
     * The overall status of the SDK's health.
     */
    val status: SDKHealthStatus,
    /**
     * The RevenueCat project identifier associated with the current SDK configuration, if available.
     */
    val projectId: String? = null,
    /**
     * The RevenueCat app identifier associated with the current SDK configuration, if available.
     */
    val appId: String? = null,
    /**
     * The report for each of your app's products set up in the RevenueCat website
     */
    val products: List<ProductDiagnosticsPayload> = emptyList(),
    /**
     * The report for each of your app's offerings set up in the RevenueCat website
     */
    val offerings: List<OfferingDiagnosticsPayload> = emptyList()
)

/**
 * Status of the SDK Health report
 */
sealed class SDKHealthStatus {
    /**
     * SDK configuration is valid but might have some non-blocking issues
     */
    data class Healthy(val warnings: List<SDKHealthError>) : SDKHealthStatus()
    
    /**
     * SDK configuration is not valid and has issues that must be resolved
     */
    data class Unhealthy(val error: SDKHealthError) : SDKHealthStatus()
}

/**
 * Error types that can occur during SDK health checks
 */
sealed class SDKHealthError {
    /**
     * The SDK is not authorized to make payments
     */
    object NotAuthorizedToMakePayments : SDKHealthError()
    
    /**
     * The API key is invalid
     */
    object InvalidAPIKey : SDKHealthError()
    
    /**
     * No offerings are configured
     */
    object NoOfferings : SDKHealthError()
    
    /**
     * Bundle ID configuration issue
     */
    data class InvalidBundleId(val payload: InvalidBundleIdErrorPayload?) : SDKHealthError()
    
    /**
     * Product configuration issues
     */
    data class InvalidProducts(val products: List<ProductDiagnosticsPayload>) : SDKHealthError()
    
    /**
     * Offering configuration issues
     */
    data class OfferingConfiguration(val offerings: List<OfferingDiagnosticsPayload>) : SDKHealthError()
    
    /**
     * Unknown error occurred
     */
    data class Unknown(val error: Exception) : SDKHealthError()
}

/**
 * Product diagnostics payload containing product configuration information
 */
data class ProductDiagnosticsPayload(
    val productId: String,
    val status: String,
    val name: String? = null,
    val description: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val subscriptionPeriod: String? = null,
    val trialPeriod: String? = null,
    val introPeriod: String? = null,
    val introPrice: String? = null
)

/**
 * Offering diagnostics payload containing offering configuration information
 */
data class OfferingDiagnosticsPayload(
    val offeringId: String,
    val status: String,
    val packages: List<OfferingPackageDiagnosticsPayload> = emptyList()
)

/**
 * Package diagnostics payload containing package configuration information
 */
data class OfferingPackageDiagnosticsPayload(
    val packageId: String,
    val status: String,
    val productId: String? = null
)

/**
 * Bundle ID error payload containing bundle ID mismatch information
 */
data class InvalidBundleIdErrorPayload(
    val appBundleId: String? = null,
    val sdkBundleId: String? = null
)