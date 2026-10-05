// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.CreateSupportTicketResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import kotlinx.coroutines.flow.StateFlow

@Suppress("TooManyFunctions")
@InternalRevenueCatAPI
public interface CustomerCenterPreviewProvider {

    public val appUserID: String
    public val store: Store
    public val storefrontCountryCode: String? get() = null
    public val preferredUILocaleOverride: String? get() = null
    public val customerInfoUpdates: StateFlow<CustomerInfo>? get() = null

    public fun onDiagnosticsUpdated(diagnostics: List<CustomerCenterPreviewDiagnostic>): Unit = Unit

    public suspend fun workflow(offering: Offering): CustomerCenterPreviewWorkflow? = null

    public suspend fun customerInfo(): CustomerInfo

    public suspend fun configuration(): CustomerCenterConfigData

    public suspend fun product(productId: String, basePlan: String?): StoreProduct?

    public suspend fun offerings(): Offerings

    public suspend fun purchase(params: PurchaseParams.Builder): PurchaseResult

    public suspend fun restorePurchases(): CustomerInfo

    public suspend fun createSupportTicket(email: String, description: String): CreateSupportTicketResult

    public suspend fun virtualCurrencies(): VirtualCurrencies = VirtualCurrencies(emptyMap())

    public suspend fun handleAction(action: CustomerCenterPreviewAction)
}

@InternalRevenueCatAPI
public sealed interface CustomerCenterPreviewAction {

    public data class ManageSubscriptions(val productId: String?) : CustomerCenterPreviewAction

    public data class RequestRefund(val productId: String?) : CustomerCenterPreviewAction

    public data class ChangePlans(val productId: String?) : CustomerCenterPreviewAction

    public data class ShowPaywall(val offering: Offering) : CustomerCenterPreviewAction

    public data class OpenUrl(val url: String) : CustomerCenterPreviewAction

    public data class CustomAction(val actionIdentifier: String, val productId: String?) : CustomerCenterPreviewAction

    public data class ContactSupport(val email: String) : CustomerCenterPreviewAction
}

@InternalRevenueCatAPI
public data class CustomerCenterPreviewWorkflow(
    public val workflow: PublishedWorkflow,
    public val offerings: Offerings,
    public val uiConfig: UiConfig,
)

@InternalRevenueCatAPI
public data class CustomerCenterPreviewDiagnostic(
    public val pathId: String,
    public val title: String,
    public val productId: String?,
    public val visible: Boolean,
    public val reason: CustomerCenterPreviewDiagnosticReason? = null,
    public val detail: String? = null,
)

@InternalRevenueCatAPI
public enum class CustomerCenterPreviewDiagnosticReason {
    UNSUPPORTED_STORE,
    NO_PURCHASE_SELECTED,
    REFUND_UNAVAILABLE_DURING_TRIAL,
    REFUND_REQUIRES_PAID_PURCHASE,
    ACTIVE_SUBSCRIPTION_REQUIRED,
    PLAN_CHANGE_UNAVAILABLE_FOR_FAMILY_SHARED,
    SUBSCRIPTION_REQUIRED,
    PROMOTIONAL_OFFER_INELIGIBLE,
    TARGET_PRODUCT_NOT_FOUND,
    PROMOTIONAL_OFFER_NOT_FOUND,
    REFUND_WINDOW_EXCEEDED,
    WORKFLOW_USED_AFTER_OPENING_PAYWALL,
}
