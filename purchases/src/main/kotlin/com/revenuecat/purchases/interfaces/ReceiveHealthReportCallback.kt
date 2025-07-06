package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.utils.SDKHealthReport

/**
 * Interface to be implemented when making calls to get health report.
 */
interface ReceiveHealthReportCallback {
    /**
     * Called when the health report is received successfully.
     * @param healthReport The health report containing SDK configuration status
     */
    fun onReceived(healthReport: SDKHealthReport)

    /**
     * Called when there's an error retrieving the health report.
     * @param error The error that occurred
     */
    fun onError(error: PurchasesError)
}