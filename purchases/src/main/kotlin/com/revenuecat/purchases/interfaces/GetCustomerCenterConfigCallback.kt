package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData

interface GetCustomerCenterConfigCallback {
    /**
     * Will be called after the call has completed.
     * @param customerCenterConfig config for the customer center to be created
     */
    fun onSuccess(customerCenterConfig: CustomerCenterConfigData)

    /**
     * Will be called after the call has completed with an error.
     * @param error A [PurchasesError] containing the reason for the failure of the call
     */
    fun onError(error: PurchasesError)
}
