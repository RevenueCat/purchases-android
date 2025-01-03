package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedPeriod
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.utils.getDefaultLocales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("TooManyFunctions")
internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
    suspend fun pathButtonPressed(context: Context, path: CustomerCenterConfigData.HelpPath, product: StoreProduct?)
    fun dismissRestoreDialog()
    suspend fun restorePurchases()
    fun contactSupport(context: Context, supportEmail: String)
    fun loadAndDisplayPromotionalOffer(
        product: StoreProduct,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        onAccepted: () -> Unit,
        onDismiss: () -> Unit,
    )
    fun dismissPromotionalOffer()
    fun onNavigationButtonPressed()
    suspend fun loadCustomerCenter()
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("TooManyFunctions")
internal class CustomerCenterViewModelImpl(
    private val purchases: PurchasesType,
) : ViewModel(), CustomerCenterViewModel {
    companion object {
        private const val STOP_FLOW_TIMEOUT = 5_000L
    }

    private val _state = MutableStateFlow<CustomerCenterState>(CustomerCenterState.NotLoaded)
    override val state = _state
        .onStart {
            loadCustomerCenter()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_FLOW_TIMEOUT),
            initialValue = CustomerCenterState.Loading,
        )

    override suspend fun pathButtonPressed(
        context: Context,
        path: CustomerCenterConfigData.HelpPath,
        product: StoreProduct?,
    ) {
        path.feedbackSurvey?.let { feedbackSurvey ->
            displayFeedbackSurvey(feedbackSurvey, onAnswerSubmitted = { option ->
                goBackToMain()
                option?.let {
                    promotionalOfferOrMainPathAction(product, option.promotionalOffer, path, context)
                }
            })
            return
        }
        promotionalOfferOrMainPathAction(product, path.promotionalOffer, path, context)
    }

    private fun promotionalOfferOrMainPathAction(
        product: StoreProduct?,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer?,
        path: CustomerCenterConfigData.HelpPath,
        context: Context,
    ) {
        if (product == null || promotionalOffer == null) {
            mainPathAction(path, context)
            return
        }
        loadAndDisplayPromotionalOffer(
            product,
            promotionalOffer,
            onAccepted = {
                Log.d("CustomerCenter", "Promotional offer accepted")
                goBackToMain()
            },
            onDismiss = {
                mainPathAction(path, context)
            },
        )
    }

    private fun mainPathAction(
        path: CustomerCenterConfigData.HelpPath,
        context: Context,
    ) {
        when (path.type) {
            CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE -> {
                _state.update { currentState ->
                    when (currentState) {
                        is CustomerCenterState.Success -> {
                            currentState.copy(showRestoreDialog = true)
                        }

                        else -> currentState
                    }
                }
            }

            CustomerCenterConfigData.HelpPath.PathType.CANCEL -> {
                when (val currentState = _state.value) {
                    is CustomerCenterState.Success -> {
                        currentState.purchaseInformation?.product?.let {
                            showManageSubscriptions(context, it.id)
                        }
                    }

                    else -> {}
                }
            }

            else -> {
                // Other cases are not supported
            }
        }
    }

    override fun dismissRestoreDialog() {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(
                    showRestoreDialog = false,
                    restorePurchasesState = RestorePurchasesState.INITIAL,
                )
            } else {
                currentState
            }
        }
    }

    override suspend fun restorePurchases() {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(
                    restorePurchasesState = RestorePurchasesState.RESTORING,
                )
            } else {
                currentState
            }
        }
        try {
            val customerInfo = purchases.awaitRestore()
            val hasPurchases =
                customerInfo.activeSubscriptions.isNotEmpty() ||
                    customerInfo.nonSubscriptionTransactions.isNotEmpty()
            _state.update { currentState ->
                if (currentState is CustomerCenterState.Success) {
                    currentState.copy(
                        restorePurchasesState = if (hasPurchases) {
                            RestorePurchasesState.PURCHASES_RECOVERED
                        } else {
                            RestorePurchasesState.PURCHASES_NOT_FOUND
                        },
                    )
                } else {
                    currentState
                }
            }
        } catch (e: PurchasesException) {
            Logger.e("Error restoring purchases", e)
            _state.update { currentState ->
                if (currentState is CustomerCenterState.Success) {
                    currentState.copy(
                        restorePurchasesState = RestorePurchasesState.PURCHASES_NOT_FOUND,
                    )
                } else {
                    currentState
                }
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun loadPurchaseInformation(): PurchaseInformation? {
        val customerInfo = purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)

        // Customer Center WIP: update when we have subscription information in CustomerInfo
        val activeEntitlement = customerInfo.entitlements.active.values.firstOrNull()
        if (activeEntitlement != null) {
            val product =
                purchases.awaitGetProduct(activeEntitlement.productIdentifier, activeEntitlement.productPlanIdentifier)
                    ?: return null
            val locale = getDefaultLocales().first()
            val purchaseInformation = PurchaseInformation(
                title = product?.description ?: "",
                durationTitle = product?.period?.localizedPeriod(locale) ?: "",
                price = product?.price?.formatted ?: "",
                expirationDateString = activeEntitlement.expirationDate.toString(),
                willRenew = activeEntitlement.willRenew,
                active = activeEntitlement.isActive,
                product = product,
            )
            return purchaseInformation
        }

        return null
    }

    override fun contactSupport(context: Context, supportEmail: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$supportEmail")
            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            putExtra(Intent.EXTRA_TEXT, "Support request details...")
        }
        context.startActivity(Intent.createChooser(intent, "Contact Support"))
    }

    override fun loadAndDisplayPromotionalOffer(
        product: StoreProduct,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        onAccepted: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val offerIdentifier = promotionalOffer.productMapping[product.id]
        val offer = product.subscriptionOptions?.firstOrNull { option -> option.id == offerIdentifier }
        if (offer != null) {
            _state.update {
                val currentState = _state.value
                if (currentState is CustomerCenterState.Success) {
                    currentState.copy(
                        promotionalOfferData = PromotionalOfferData(promotionalOffer, offer, onAccepted, onDismiss),
                    )
                } else {
                    currentState
                }
            }
        }
    }

    override fun onNavigationButtonPressed() {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success &&
                currentState.navigationButtonType == CustomerCenterState.NavigationButtonType.BACK
            ) {
                currentState.copy(
                    feedbackSurveyData = null,
                    showRestoreDialog = false,
                    navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
                )
            } else {
                CustomerCenterState.NotLoaded
            }
        }
    }

    override fun dismissPromotionalOffer() {
        _state.update {
            val currentState = _state.value
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(promotionalOfferData = null)
            } else {
                currentState
            }
        }
    }

    override suspend fun loadCustomerCenter() {
        if (_state.value !is CustomerCenterState.Loading) {
            _state.value = CustomerCenterState.Loading
        }
        try {
            val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
            val purchaseInformation = loadPurchaseInformation()
            _state.value = CustomerCenterState.Success(customerCenterConfigData, purchaseInformation)
        } catch (e: PurchasesException) {
            _state.value = CustomerCenterState.Error(e.error)
        }
    }

    private fun displayFeedbackSurvey(
        feedbackSurvey: CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey,
        onAnswerSubmitted: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
    ) {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(
                    feedbackSurveyData = FeedbackSurveyData(feedbackSurvey, onAnswerSubmitted),
                    title = feedbackSurvey.title,
                    navigationButtonType = CustomerCenterState.NavigationButtonType.BACK,
                )
            } else {
                currentState
            }
        }
    }

    private fun goBackToMain() {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(
                    feedbackSurveyData = null,
                    promotionalOfferData = null,
                    showRestoreDialog = false,
                    title = null,
                    navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
                )
            } else {
                currentState
            }
        }
    }

    private fun showManageSubscriptions(context: Context, productId: String) {
        try {
            val packageName = context.packageName
            val uri = "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            Logger.e("Error opening manage subscriptions", e)
        }
    }
}
