package com.revenuecat.purchases.ui.revenuecatui.customercenter

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Contract for launching the Customer Center.
 *
 * Usage:
 * ```
 * class MyActivity : ComponentActivity() {
 *     private val customerCenter = registerForActivityResult(ShowCustomerCenter()) {
 *         // Handle the dismissal
 *     }
 *
 *     fun showCustomerCenter() {
 *         customerCenter.launch()
 *     }
 * }
 * ```
 */
class ShowCustomerCenter : ActivityResultContract<Unit, Unit>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return CustomerCenterActivity.createIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        // No result to parse since Customer Center doesn't return any data
    }
}
