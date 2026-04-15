@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.toPurchasesError
import java.io.IOException

internal class WorkflowManager(
    private val backend: Backend,
    private val workflowDetailResolver: WorkflowDetailResolver,
) {

    fun getWorkflows(
        appUserID: String,
        appInBackground: Boolean,
        onSuccess: (WorkflowsListResponse) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        backend.getWorkflows(
            appUserID = appUserID,
            appInBackground = appInBackground,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    fun getWorkflow(
        appUserID: String,
        workflowId: String,
        appInBackground: Boolean,
        onSuccess: (WorkflowFetchResult) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        backend.getWorkflow(
            appUserID = appUserID,
            workflowId = workflowId,
            appInBackground = appInBackground,
            onSuccess = { response ->
                try {
                    onSuccess(workflowDetailResolver.resolve(response))
                } catch (e: IllegalStateException) {
                    onError(e.toPurchasesError())
                } catch (e: IOException) {
                    onError(e.toPurchasesError())
                }
            },
            onError = onError,
        )
    }
}
