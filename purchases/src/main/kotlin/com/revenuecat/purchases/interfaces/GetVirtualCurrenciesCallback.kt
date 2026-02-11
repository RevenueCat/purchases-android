package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies

/**
 * Interface to be implemented when making calls that return a [VirtualCurrencies]
 */
public interface GetVirtualCurrenciesCallback {
    /**
     * Will be called after the call has completed.
     * @param virtualCurrencies [VirtualCurrencies] class sent back when the call has completed
     */
    fun onReceived(virtualCurrencies: VirtualCurrencies)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    fun onError(error: PurchasesError)
}
