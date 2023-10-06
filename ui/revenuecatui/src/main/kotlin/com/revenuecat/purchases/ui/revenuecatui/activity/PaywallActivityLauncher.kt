package com.revenuecat.purchases.ui.revenuecatui.activity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.Offering

/**
 * Implement this interface to receive the result of the paywall activity.
 */
interface PaywallResultHandler: ActivityResultCallback<PaywallResult>

/**
 * Launches the paywall activity. You need to create this object during the activity's onCreate.
 * Then launch the activity at your moment of choice
 */
class PaywallActivityLauncher {

    private var activityResultLauncher: ActivityResultLauncher<PaywallActivityArgs>

    /**
     * Creates a new PaywallActivityLauncher from an activity.
     */
    constructor(activity: ComponentActivity, resultHandler: PaywallResultHandler) {
        activityResultLauncher = activity.registerForActivityResult(PaywallContract(), resultHandler)
    }

    /**
     * Creates a new PaywallActivityLauncher from a fragment.
     */
    constructor(fragment: Fragment, resultHandler: PaywallResultHandler) {
        activityResultLauncher = fragment.registerForActivityResult(PaywallContract(), resultHandler)
    }

    /**
     * Launch the paywall activity.
     * @param offering The offering to be shown in the paywall. If null, the current offering will be shown.
     */
    fun launch(offering: Offering? = null) {
        activityResultLauncher.launch(PaywallActivityArgs(offeringId = offering?.identifier))
    }
}
