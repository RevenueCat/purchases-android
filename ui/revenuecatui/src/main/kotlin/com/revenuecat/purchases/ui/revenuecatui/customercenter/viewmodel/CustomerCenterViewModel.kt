package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.common.SharedConstants
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.extensions.getLocalizedDescription
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.utils.DateFormatter
import com.revenuecat.purchases.ui.revenuecatui.utils.DefaultDateFormatter
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpener
import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpeningMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("TooManyFunctions")
internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
    val actionError: State<PurchasesError?>

    suspend fun pathButtonPressed(
        context: Context,
        path: CustomerCenterConfigData.HelpPath,
        product: StoreProduct?,
    )

    fun dismissRestoreDialog()
    suspend fun restorePurchases()
    fun contactSupport(context: Context, supportEmail: String)
    fun loadAndDisplayPromotionalOffer(
        product: StoreProduct,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        originalPath: CustomerCenterConfigData.HelpPath,
    )

    suspend fun onAcceptedPromotionalOffer(subscriptionOption: SubscriptionOption, activity: Activity?)
    fun dismissPromotionalOffer(originalPath: CustomerCenterConfigData.HelpPath, context: Context)
    fun onNavigationButtonPressed()
    suspend fun loadCustomerCenter()
    fun openURL(
        context: Context,
        url: String,
        method: CustomerCenterConfigData.HelpPath.OpenMethod = CustomerCenterConfigData.HelpPath.OpenMethod.EXTERNAL,
    )
    fun clearActionError()
}

internal sealed class TransactionDetails(
    open val productIdentifier: String,
    open val store: Store,
) {
    data class Subscription(
        override val productIdentifier: String,
        override val store: Store,
        val isActive: Boolean,
        val willRenew: Boolean,
        val expiresDate: Date?,
    ) : TransactionDetails(productIdentifier, store)

    data class NonSubscription(
        override val productIdentifier: String,
        override val store: Store,
    ) : TransactionDetails(productIdentifier, store)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("TooManyFunctions")
internal class CustomerCenterViewModelImpl(
    private val purchases: PurchasesType,
    private val dateFormatter: DateFormatter = DefaultDateFormatter(),
    private val locale: Locale = Locale.getDefault(),
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

    override val actionError: State<PurchasesError?>
        get() = _actionError
    private val _actionError: MutableState<PurchasesError?> = mutableStateOf(null)

    override suspend fun pathButtonPressed(
        context: Context,
        path: CustomerCenterConfigData.HelpPath,
        product: StoreProduct?,
    ) {
        path.feedbackSurvey?.let { feedbackSurvey ->
            displayFeedbackSurvey(feedbackSurvey, onAnswerSubmitted = { option ->
                goBackToMain()
                option?.let {
                    if (product != null && it.promotionalOffer != null) {
                        loadAndDisplayPromotionalOffer(
                            product,
                            it.promotionalOffer!!,
                            path,
                        )
                    } else {
                        mainPathAction(path, context)
                    }
                }
            })
            return
        }

        if (product != null && path.promotionalOffer != null) {
            loadAndDisplayPromotionalOffer(
                product,
                path.promotionalOffer!!,
                path,
            )
        } else {
            mainPathAction(path, context)
        }
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

            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL -> {
                path.url?.let {
                    openURL(
                        context,
                        it,
                        path.openMethod ?: CustomerCenterConfigData.HelpPath.OpenMethod.EXTERNAL,
                    )
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

    private suspend fun loadPurchaseInformation(
        dateFormatter: DateFormatter,
        locale: Locale,
    ): PurchaseInformation? {
        val customerInfo = purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)

        val hasActiveSubscriptions = customerInfo.activeSubscriptions.isNotEmpty()
        val hasNonSubscriptionTransactions = customerInfo.nonSubscriptionTransactions.isNotEmpty()

        if (hasActiveSubscriptions || hasNonSubscriptionTransactions) {
            val activeTransactionDetails = findActiveTransaction(customerInfo)

            if (activeTransactionDetails != null) {
                val entitlement = customerInfo.entitlements.all.values
                    .firstOrNull { it.productIdentifier == activeTransactionDetails.productIdentifier }

                return createPurchaseInformation(
                    activeTransactionDetails,
                    entitlement,
                    customerInfo.managementURL,
                    dateFormatter,
                    locale,
                )
            } else {
                Logger.w("Could not find subscription information")
            }
        }

        return null
    }

    private fun findActiveTransaction(customerInfo: CustomerInfo): TransactionDetails? {
        val activeSubscriptions = customerInfo.subscriptionsByProductIdentifier.values
            .filter { it.isActive }
            .sortedBy { it.expiresDate }

        val activeGoogleSubscriptions = activeSubscriptions.filter { it.store == Store.PLAY_STORE }
        val googleNonSubscriptions = customerInfo.nonSubscriptionTransactions.filter { it.store == Store.PLAY_STORE }
        val otherActiveSubscriptions = activeSubscriptions.filter { it.store != Store.PLAY_STORE }
        val otherNonSubscriptions = customerInfo.nonSubscriptionTransactions.filter { it.store != Store.PLAY_STORE }

        val transaction = activeGoogleSubscriptions.firstOrNull()
            ?: googleNonSubscriptions.firstOrNull()
            ?: otherActiveSubscriptions.firstOrNull()
            ?: otherNonSubscriptions.firstOrNull()

        return transaction?.let {
            when (it) {
                is SubscriptionInfo -> TransactionDetails.Subscription(
                    productIdentifier = it.productIdentifier,
                    store = it.store,
                    isActive = it.isActive,
                    willRenew = it.willRenew,
                    expiresDate = it.expiresDate,
                )

                is Transaction -> TransactionDetails.NonSubscription(
                    productIdentifier = it.productIdentifier,
                    store = it.store,
                )

                else -> null
            }
        }
    }

    private suspend fun createPurchaseInformation(
        transaction: TransactionDetails,
        entitlement: EntitlementInfo?,
        managementURL: Uri?,
        dateFormatter: DateFormatter,
        locale: Locale,
    ): PurchaseInformation {
        val product = if (transaction.store == Store.PLAY_STORE) {
            purchases.awaitGetProduct(
                transaction.productIdentifier,
                entitlement?.productPlanIdentifier,
            ).also {
                if (it == null) {
                    Logger.w(
                        "Could not find product, loading without product information: ${transaction.productIdentifier}",
                    )
                }
            }
        } else {
            Logger.w("Active product is not from Google, loading without product information: ${transaction.store}")
            null
        }

        return PurchaseInformation(
            entitlementInfo = entitlement,
            subscribedProduct = product,
            transaction = transaction,
            managementURL = managementURL,
            dateFormatter = dateFormatter,
            locale = locale,
        )
    }

    override fun contactSupport(context: Context, supportEmail: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$supportEmail")
            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            putExtra(Intent.EXTRA_TEXT, "Support request details...")
        }
        context.startActivity(Intent.createChooser(intent, "Contact Support"))
    }

    @SuppressWarnings("ForbiddenComment")
    override fun openURL(context: Context, url: String, method: CustomerCenterConfigData.HelpPath.OpenMethod) {
        val openingMethod = when (method) {
            CustomerCenterConfigData.HelpPath.OpenMethod.IN_APP -> URLOpeningMethod.IN_APP_BROWSER
            CustomerCenterConfigData.HelpPath.OpenMethod.EXTERNAL,
            -> URLOpeningMethod.EXTERNAL_BROWSER
        }
        URLOpener.openURL(context, url, openingMethod)
    }

    override fun clearActionError() {
        _actionError.value = null
    }

    override fun loadAndDisplayPromotionalOffer(
        product: StoreProduct,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        originalPath: CustomerCenterConfigData.HelpPath,
    ) {
        val offerIdentifier = promotionalOffer.productMapping[product.id]
        val subscriptionOption = product.subscriptionOptions?.firstOrNull { option ->
            when (option) {
                is GoogleSubscriptionOption ->
                    option.tags.contains(SharedConstants.RC_CUSTOMER_CENTER_TAG) && option.offerId == offerIdentifier

                else -> false
            }
        }
        if (subscriptionOption != null) {
            _state.update {
                val currentState = _state.value
                if (currentState is CustomerCenterState.Success) {
                    val localization = currentState.customerCenterConfigData.localization
                    val pricingPhasesDescription = subscriptionOption.getLocalizedDescription(localization, locale)
                    currentState.copy(
                        promotionalOfferData = PromotionalOfferData(
                            promotionalOffer,
                            subscriptionOption,
                            originalPath,
                            pricingPhasesDescription,
                        ),
                    )
                } else {
                    currentState
                }
            }
        }
    }

    override suspend fun onAcceptedPromotionalOffer(subscriptionOption: SubscriptionOption, activity: Activity?) {
        if (activity == null) {
            Logger.e("Activity is null when accepting promotional offer")
            _actionError.value = PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "Couldn't perform purchase",
            )
            return
        }
        val purchaseParams = PurchaseParams.Builder(activity, subscriptionOption)
        try {
            purchases.awaitPurchase(purchaseParams)
            goBackToMain()
        } catch (e: PurchasesException) {
            if (e.code != PurchasesErrorCode.PurchaseCancelledError) {
                _actionError.value = e.error
                goBackToMain()
            }
        }
    }

    override fun dismissPromotionalOffer(originalPath: CustomerCenterConfigData.HelpPath, context: Context) {
        // Continue with the original action and remove the promotional offer data
        mainPathAction(originalPath, context)

        _state.update {
            val currentState = _state.value
            if (currentState is CustomerCenterState.Success) {
                currentState.copy(promotionalOfferData = null)
            } else {
                currentState
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

    override suspend fun loadCustomerCenter() {
        if (_state.value !is CustomerCenterState.Loading) {
            _state.value = CustomerCenterState.Loading
        }
        try {
            val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
            val purchaseInformation = loadPurchaseInformation(dateFormatter, locale)
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
