// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.workflows.WorkflowActionID
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowStepID
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.storage.FileRepository
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import java.net.URI
import java.net.URL

/** Adapts a preview session without accessing Purchases.sharedInstance or emitting SDK events. */
@Suppress("TooManyFunctions")
internal class CustomerCenterPreviewPurchases(private val provider: CustomerCenterPreviewProvider) : PurchasesType {
    override val fileRepositoryOverride: FileRepository = StreamingPreviewFiles
    override val appUserID: String get() = provider.appUserID
    override val store: Store get() = provider.store
    override val storefrontCountryCode: String? get() = provider.storefrontCountryCode
    override val preferredUILocaleOverride: String? get() = provider.preferredUILocaleOverride
    override val purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
    override val customerCenterListener: CustomerCenterListener? = null
    override suspend fun awaitCustomerInfo(fetchPolicy: CacheFetchPolicy) = provider.customerInfo()
    override suspend fun awaitCustomerCenterConfigData() = provider.configuration()
    override suspend fun awaitGetProduct(productId: String, basePlan: String?) = provider.product(productId, basePlan)
    override suspend fun awaitOfferings() = provider.offerings()
    override suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder) = provider.purchase(purchaseParams)
    override suspend fun awaitRestore() = provider.restorePurchases()
    override suspend fun awaitSyncPurchases(): CustomerInfo = provider.customerInfo()
    override suspend fun awaitGetVirtualCurrencies() = provider.virtualCurrencies()
    override suspend fun awaitCreateSupportTicket(email: String, description: String) =
        provider.createSupportTicket(email, description)
    override fun invalidateVirtualCurrenciesCache() = Unit
    override fun track(event: FeatureEvent) = Unit
    override suspend fun awaitGetWorkflow(workflowId: String): Nothing = unsupportedWorkflow()
    override suspend fun awaitGetUiConfig(): Nothing = unsupportedWorkflow()
    override suspend fun resolveWorkflow(offeringId: String): WorkflowResolution = WorkflowResolution.NoWorkflow
    override suspend fun awaitWorkflowBlobRef(workflowId: String): Nothing = unsupportedWorkflow()

    // A preview has no audiences to evaluate, so every branch takes its fallback.
    override suspend fun resolveBranches(
        step: WorkflowStep,
        customVariables: Map<String, RulesDimensionValue>,
    ): Map<WorkflowActionID, WorkflowStepID> = emptyMap()

    private fun unsupportedWorkflow(): Nothing = error("Preview paywalls must be hosted by the preview action handler")
}

/** Preview videos stream their public asset URLs without initializing billing or a global SDK session. */
private object StreamingPreviewFiles : FileRepository {
    override fun prefetch(urls: List<Pair<URL, Checksum?>>) = Unit
    override suspend fun generateOrGetCachedFileURL(url: URL, checksum: Checksum?): URI = url.toURI()
    override fun getFile(url: URL, checksum: Checksum?): URI? = null
}
