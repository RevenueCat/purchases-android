package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
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
internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
    suspend fun pathButtonPressed(context: Context, path: CustomerCenterConfigData.HelpPath)
    fun dismissRestoreDialog()
    suspend fun restorePurchases()
    fun contactSupport(context: Context, supportEmail: String)
    fun openAppStore(context: Context)
    fun showManageSubscriptions(context: Context, productId: String)
    fun displayFeedbackSurvey(
        path: CustomerCenterConfigData.HelpPath,
        onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
    )
    fun dismissFeedbackSurvey()
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class CustomerCenterViewModelImpl(
    private val purchases: PurchasesType,
) : ViewModel(), CustomerCenterViewModel {
    companion object {
        private const val STOP_FLOW_TIMEOUT = 5_000L
    }

    private val _state = MutableStateFlow<CustomerCenterState>(CustomerCenterState.Loading)
    override val state = _state
        .onStart {
            try {
                val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
                val purchaseInformation = loadPurchaseInformation()
                _state.value = CustomerCenterState.Success(customerCenterConfigData, purchaseInformation)
            } catch (e: PurchasesException) {
                _state.value = CustomerCenterState.Error(e.error)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_FLOW_TIMEOUT),
            initialValue = CustomerCenterState.Loading,
        )

    override suspend fun pathButtonPressed(context: Context, path: CustomerCenterConfigData.HelpPath) {
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
                        currentState.purchaseInformation?.productId?.let {
                            showManageSubscriptions(context, it)
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
        _state.update {
            val currentState = _state.value
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
        val currentState = _state.value as? CustomerCenterState.Success ?: return

        try {
            _state.value = currentState.copy(
                restorePurchasesState = RestorePurchasesState.RESTORING,
            )
            val customerInfo = purchases.awaitRestore()
            val hasPurchases =
                customerInfo.activeSubscriptions.isNotEmpty() ||
                    customerInfo.nonSubscriptionTransactions.isNotEmpty()
            if (hasPurchases) {
                _state.value = currentState.copy(
                    restorePurchasesState = RestorePurchasesState.PURCHASES_RECOVERED,
                )
            } else {
                _state.value = currentState.copy(
                    restorePurchasesState = RestorePurchasesState.PURCHASES_NOT_FOUND,
                )
            }
        } catch (e: PurchasesException) {
            Logger.e("Error restoring purchases", e)
            _state.value = currentState.copy(
                restorePurchasesState = RestorePurchasesState.PURCHASES_NOT_FOUND,
            )
        }
    }

    private suspend fun loadPurchaseInformation(): PurchaseInformation? {
        val customerInfo = purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)

        // Customer Center WIP: update when we have subscription information in CustomerInfo
        val activeEntitlement = customerInfo.entitlements.active.values.firstOrNull()
        if (activeEntitlement != null) {
            val product = purchases.awaitGetProduct(activeEntitlement.productIdentifier).first()
            val locale = getDefaultLocales().first()
            val purchaseInformation = PurchaseInformation(
                title = product.description,
                durationTitle = product.period?.localizedPeriod(locale) ?: "",
                price = product.price.formatted,
                expirationDateString = activeEntitlement.expirationDate.toString(),
                willRenew = activeEntitlement.willRenew,
                active = activeEntitlement.isActive,
                productId = activeEntitlement.productIdentifier,
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

    override fun openAppStore(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=${context.packageName}")
        }
        context.startActivity(intent)
    }

    override fun showManageSubscriptions(context: Context, productId: String) {
        try {
            val packageName = context.packageName
            val uri = "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            Logger.e("Error opening manage subscriptions", e)
        }
    }

    override fun displayFeedbackSurvey(
        path: CustomerCenterConfigData.HelpPath,
        onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
    ) {
        _state.update {
            val currentState = _state.value
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(
                    feedbackSurveyData = FeedbackSurveyData(path, onOptionSelected),
                )
            } else {
                currentState
            }
        }
    }

    override fun dismissFeedbackSurvey() {
        _state.update {
            val currentState = _state.value
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(feedbackSurveyData = null)
            } else {
                currentState
            }
        }
    }
}
