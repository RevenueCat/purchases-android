package com.revenuecat.purchases.ui.revenuecatui.customercenter

/**
 * Implement this interface to receive events from the Customer Center.
 * 
 * IMPORTANT: This handler should be set at the Application level using [Purchases.setCustomerCenterEventHandler].
 * It should be used for analytics and logging purposes, not for UI updates.
 * 
 * Example:
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         
 *         // Set up analytics handler at the application level
 *         Purchases.setCustomerCenterEventHandler(object : CustomerCenterEventHandler {
 *             override fun onRestoreStarted() {
 *                 Analytics.track("customer_center_restore_started")
 *             }
 *         })
 *     }
 * }
 * ```
 */
internal interface CustomerCenterEventHandler {
    /**
     * Called when the user starts the restore purchases process in the Customer Center.
     */
    fun onRestoreStarted() {
        // Default empty implementation
    }
} 