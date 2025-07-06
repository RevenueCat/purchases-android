package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.responses.HealthReport
import com.revenuecat.purchases.interfaces.ReceiveHealthReportCallback
import com.revenuecat.purchases.utils.SDKHealthError
import com.revenuecat.purchases.utils.SDKHealthReport
import com.revenuecat.purchases.utils.SDKHealthStatus
import com.revenuecat.purchases.utils.OfferingDiagnosticsPayload
import com.revenuecat.purchases.utils.OfferingPackageDiagnosticsPayload
import com.revenuecat.purchases.utils.ProductDiagnosticsPayload
import com.revenuecat.purchases.utils.InvalidBundleIdErrorPayload
import com.revenuecat.purchases.common.responses.HealthCheckStatus
import com.revenuecat.purchases.common.responses.HealthCheckType
import com.revenuecat.purchases.common.responses.HealthCheck
import com.revenuecat.purchases.identity.IdentityManager
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Helper class for health report functionality.
 * Handles the business logic for SDK health checks including payment authorization,
 * network requests, and response transformation.
 */
internal class HealthReportHelper(
    private val backend: Backend,
    private val identityManager: IdentityManager,
    private val dispatcher: Dispatcher,
    private val diagnosticsTracker: DiagnosticsTracker?,
) {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper())

    /**
     * Retrieves the health report for the current SDK configuration.
     * 
     * @param callback The callback to receive the health report result
     */
    fun retrieveHealthReport(callback: ReceiveHealthReportCallback) {
        // Track health report started
        diagnosticsTracker?.trackHealthReportStarted()
        
        // Check if payments are allowed first
        if (!canMakePayments()) {
            diagnosticsTracker?.trackHealthReportResult(
                success = false,
                error = "not_authorized_to_make_payments"
            )
            callback.onReceived(
                SDKHealthReport(
                    status = SDKHealthStatus.Unhealthy(SDKHealthError.NotAuthorizedToMakePayments)
                )
            )
            return
        }

        // Fetch health report from backend
        backend.getHealthReport(
            appUserID = identityManager.currentAppUserID,
            appInBackground = false, // Health checks should be foreground operations
            onSuccess = { jsonObject ->
                try {
                    val healthReport = parseHealthReport(jsonObject)
                    val sdkHealthReport = transformToSDKHealthReport(healthReport)
                    
                    // Track successful health report
                    diagnosticsTracker?.trackHealthReportResult(
                        success = true,
                        status = when (sdkHealthReport.status) {
                            is SDKHealthStatus.Healthy -> "healthy"
                            is SDKHealthStatus.Unhealthy -> "unhealthy"
                        }
                    )
                    
                    dispatch {
                        callback.onReceived(sdkHealthReport)
                    }
                } catch (e: Exception) {
                    // Track parsing error
                    diagnosticsTracker?.trackHealthReportResult(
                        success = false,
                        error = "parse_error",
                        errorMessage = e.message ?: "Unknown parsing error"
                    )
                    
                    dispatch {
                        callback.onError(
                            PurchasesError(
                                PurchasesErrorCode.UnknownError,
                                "Failed to parse health report: ${e.message}"
                            )
                        )
                    }
                }
            },
            onError = { error, isServerError ->
                // Track error
                diagnosticsTracker?.trackHealthReportResult(
                    success = false,
                    error = "backend_error",
                    errorCode = error.code.name,
                    errorMessage = error.message
                )
                
                // Map specific backend errors to SDK health errors
                val sdkHealthReport = when {
                    error.code == PurchasesErrorCode.InvalidCredentialsError -> {
                        SDKHealthReport(
                            status = SDKHealthStatus.Unhealthy(SDKHealthError.InvalidAPIKey)
                        )
                    }
                    else -> {
                        SDKHealthReport(
                            status = SDKHealthStatus.Unhealthy(SDKHealthError.Unknown(Exception(error.message)))
                        )
                    }
                }
                dispatch {
                    callback.onReceived(sdkHealthReport)
                }
            }
        )
    }

    /**
     * Checks if the device can make payments.
     * This is equivalent to iOS SKPaymentQueue.canMakePayments()
     */
    private fun canMakePayments(): Boolean {
        // TODO: Implement Android equivalent of iOS payment authorization check
        // For now, we'll assume payments are always allowed on Android
        // In a real implementation, this might check Google Play Services availability
        return true
    }

    /**
     * Parses the JSON response into a HealthReport object
     */
    private fun parseHealthReport(jsonObject: JSONObject): HealthReport {
        val jsonString = jsonObject.toString()
        return json.decodeFromString(HealthReport.serializer(), jsonString)
    }

    /**
     * Transforms the backend HealthReport into the public SDK HealthReport
     */
    private fun transformToSDKHealthReport(healthReport: HealthReport): SDKHealthReport {
        // Find any failed checks first
        val failedCheck = healthReport.checks.firstOrNull { it.status == HealthCheckStatus.FAILED }
        
        return if (failedCheck != null) {
            // If any check failed, the SDK is unhealthy
            SDKHealthReport(
                status = SDKHealthStatus.Unhealthy(createSDKHealthError(failedCheck)),
                projectId = healthReport.projectId,
                appId = healthReport.appId
            )
        } else {
            // If no checks failed, collect warnings and create healthy status
            val warnings = healthReport.checks
                .filter { it.status == HealthCheckStatus.WARNING }
                .map { createSDKHealthError(it) }
            
            val products = extractProductDiagnostics(healthReport)
            val offerings = extractOfferingDiagnostics(healthReport)
            
            SDKHealthReport(
                status = SDKHealthStatus.Healthy(warnings),
                projectId = healthReport.projectId,
                appId = healthReport.appId,
                products = products,
                offerings = offerings
            )
        }
    }

    /**
     * Creates an SDKHealthError from a HealthCheck
     */
    private fun createSDKHealthError(check: HealthCheck): SDKHealthError {
        return when (check.name) {
            HealthCheckType.API_KEY -> SDKHealthError.InvalidAPIKey
            HealthCheckType.BUNDLE_ID -> {
                val payload = check.details?.let { details ->
                    InvalidBundleIdErrorPayload(
                        appBundleId = details.appBundleId,
                        sdkBundleId = details.sdkBundleId
                    )
                }
                SDKHealthError.InvalidBundleId(payload)
            }
            HealthCheckType.PRODUCTS -> {
                val products = extractProductDiagnostics(check)
                SDKHealthError.InvalidProducts(products)
            }
            HealthCheckType.OFFERINGS -> SDKHealthError.NoOfferings
            HealthCheckType.OFFERINGS_PRODUCTS -> {
                val offerings = extractOfferingDiagnosticsFromCheck(check)
                SDKHealthError.OfferingConfiguration(offerings)
            }
        }
    }

    /**
     * Extracts product diagnostics from the health report
     */
    private fun extractProductDiagnostics(healthReport: HealthReport): List<ProductDiagnosticsPayload> {
        return healthReport.checks
            .filter { it.name == HealthCheckType.PRODUCTS }
            .flatMap { check -> extractProductDiagnostics(check) }
    }

    /**
     * Extracts product diagnostics from a specific health check
     */
    private fun extractProductDiagnostics(check: HealthCheck): List<ProductDiagnosticsPayload> {
        return check.details?.products?.map { product ->
            ProductDiagnosticsPayload(
                productId = product.productId,
                status = product.status.name.lowercase(),
                name = product.name,
                description = product.description,
                price = product.price,
                currency = product.currency,
                subscriptionPeriod = product.subscriptionPeriod,
                trialPeriod = product.trialPeriod,
                introPeriod = product.introPeriod,
                introPrice = product.introPrice
            )
        } ?: emptyList()
    }

    /**
     * Extracts offering diagnostics from the health report
     */
    private fun extractOfferingDiagnostics(healthReport: HealthReport): List<OfferingDiagnosticsPayload> {
        return healthReport.checks
            .filter { it.name == HealthCheckType.OFFERINGS || it.name == HealthCheckType.OFFERINGS_PRODUCTS }
            .flatMap { check -> extractOfferingDiagnosticsFromCheck(check) }
    }

    /**
     * Extracts offering diagnostics from a specific health check
     */
    private fun extractOfferingDiagnosticsFromCheck(check: HealthCheck): List<OfferingDiagnosticsPayload> {
        return check.details?.offerings?.map { offering ->
            val packages = offering.packages?.map { packageDetail ->
                OfferingPackageDiagnosticsPayload(
                    packageId = packageDetail.packageId,
                    status = packageDetail.status.name.lowercase(),
                    productId = packageDetail.productId
                )
            } ?: emptyList()
            
            OfferingDiagnosticsPayload(
                offeringId = offering.offeringId,
                status = offering.status.name.lowercase(),
                packages = packages
            )
        } ?: emptyList()
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != handler.looper.thread) {
            handler.post(action)
        } else {
            action()
        }
    }
}