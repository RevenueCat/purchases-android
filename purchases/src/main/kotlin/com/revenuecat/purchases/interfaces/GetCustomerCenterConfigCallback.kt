package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData

// Kept internal since it's not meant for public usage.
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal interface GetCustomerCenterConfigCallback {
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
