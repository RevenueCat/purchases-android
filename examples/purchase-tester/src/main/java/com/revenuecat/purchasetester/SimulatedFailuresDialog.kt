package com.revenuecat.purchasetester

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases_sample.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Lets the tester pick which API hosts the SDK should pretend are failing. The choice applies to the next
 * request, before or after the SDK is configured, and is persisted so it survives relaunches.
 */
@OptIn(InternalRevenueCatAPI::class)
fun Fragment.showSimulatedFailuresDialog(dataStoreUtils: DataStoreUtils) {
    lifecycleScope.launch {
        val configuration = dataStoreUtils.getSdkConfig().first()
        val modes = SimulatedFailuresMode.values()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.simulated_api_failures)
            .setSingleChoiceItems(
                modes.map { it.label }.toTypedArray(),
                modes.indexOf(configuration.simulatedFailuresMode),
            ) { dialog, which ->
                val mode = modes[which]
                Purchases.internalSimulatedHostFailures = mode.hostFailures()
                lifecycleScope.launch {
                    dataStoreUtils.saveSdkConfig(configuration.copy(simulatedFailuresMode = mode))
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
