package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import androidx.compose.runtime.State
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

/**
 * Shared interface for ViewModels that power component-based paywalls and workflows.
 * Both [PaywallViewModel] and [WorkflowViewModel][com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowViewModel]
 * implement this so that UI-layer code (action handlers, composables) can be reused.
 */
internal interface ComponentsViewModel {
    val actionInProgress: State<Boolean>
    val actionError: State<PurchasesError?>

    suspend fun handlePackagePurchase(activity: Activity, pkg: Package?, resolvedOffer: ResolvedOffer? = null)
    suspend fun handleRestorePurchases()
    fun getWebCheckoutUrl(action: PaywallAction.External.LaunchWebCheckout): String?
    fun invalidateCustomerInfoCache()
    fun clearActionError()
}
