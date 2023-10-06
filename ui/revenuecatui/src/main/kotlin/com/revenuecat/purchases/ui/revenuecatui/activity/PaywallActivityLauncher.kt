package com.revenuecat.purchases.ui.revenuecatui.activity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.Offering

interface PaywallResultHandler: ActivityResultCallback<PaywallResult>

class PaywallActivityLauncher {

    private var activityResultLauncher: ActivityResultLauncher<PaywallActivityArgs>

    constructor(activity: ComponentActivity, resultHandler: PaywallResultHandler) {
        activityResultLauncher = activity.registerForActivityResult(PaywallContract(), resultHandler)
    }

    constructor(fragment: Fragment, resultHandler: PaywallResultHandler) {
        activityResultLauncher = fragment.registerForActivityResult(PaywallContract(), resultHandler)
    }

    fun launch(offering: Offering? = null) {
        activityResultLauncher.launch(PaywallActivityArgs(offeringId = offering?.identifier))
    }
}
