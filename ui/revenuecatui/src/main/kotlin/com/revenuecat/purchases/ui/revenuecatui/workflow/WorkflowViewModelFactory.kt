package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider

internal class WorkflowViewModelFactory(
    private val workflowId: String,
    private val purchases: PurchasesType,
    private val resourceProvider: ResourceProvider,
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkflowViewModelImpl(
            workflowId = workflowId,
            purchases = purchases,
            resourceProvider = resourceProvider,
        ) as T
    }
}
