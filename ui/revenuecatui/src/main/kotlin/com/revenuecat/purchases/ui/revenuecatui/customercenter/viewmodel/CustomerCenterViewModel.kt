package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
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
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.events.CustomerCenterImpressionEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterSurveyOptionChosenEvent
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

    suspend fun dismissRestoreDialog()
    suspend fun restorePurchases()
    fun contactSupport(context: Context, supportEmail: String)
    fun loadAndDisplayPromotionalOffer(
        context: Context,
        product: StoreProduct,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        originalPath: CustomerCenterConfigData.HelpPath,
    ): Boolean

    suspend fun onAcceptedPromotionalOffer(subscriptionOption: SubscriptionOption, activity: Activity?)
    fun dismissPromotionalOffer(context: Context, originalPath: CustomerCenterConfigData.HelpPath)
    fun onNavigationButtonPressed(context: Context, onDismiss: () -> Unit)
    suspend fun loadCustomerCenter()
    fun openURL(
        context: Context,
        url: String,
        method: CustomerCenterConfigData.HelpPath.OpenMethod = CustomerCenterConfigData.HelpPath.OpenMethod.EXTERNAL,
    )
    fun clearActionError()

    // trigger state refresh
    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean)

    // tracks customer center impression the first time is shown
    fun trackImpressionIfNeeded()
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
    private val colorScheme: ColorScheme,
    private var isDarkMode: Boolean,
    private val listener: CustomerCenterListener? = null,
) : ViewModel(), CustomerCenterViewModel {
    companion object {
        private const val STOP_FLOW_TIMEOUT = 5_000L
    }

    private var impressionCreationData: CustomerCenterImpressionEvent.CreationData? = null
    private val _lastLocaleList = MutableStateFlow(getCurrentLocaleList())
    private val _colorScheme = MutableStateFlow(colorScheme)
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
                    trackCustomerCenterEventOptionChosen(
                        path = path.type,
                        url = path.url,
                        surveyOptionID = it.id,
                    )
                    notifyListenersForFeedbackSurveyCompleted(it.id)

                    if (product != null && it.promotionalOffer != null) {
                        val loaded = loadAndDisplayPromotionalOffer(
                            context,
                            product,
                            it.promotionalOffer!!,
                            path,
                        )
                        if (!loaded) {
                            mainPathAction(path, context)
                        }
                    } else {
                        mainPathAction(path, context)
                    }
                }
            })
            return
        }

        if (product != null && path.promotionalOffer != null) {
            val loaded = loadAndDisplayPromotionalOffer(
                context,
                product,
                path.promotionalOffer!!,
                path,
            )
            if (!loaded) {
                mainPathAction(path, context)
            }
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
                            notifyListenersForManageSubscription()
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

    override suspend fun dismissRestoreDialog() {
        loadCustomerCenter()
    }

    override suspend fun restorePurchases() {
        notifyListenersForRestoreStarted()

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

            notifyListenersForRestoreCompleted(customerInfo)

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

            notifyListenersForRestoreFailed(e.error)

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

    private fun supportedPaths(
        purchaseInformation: PurchaseInformation?,
        screen: CustomerCenterConfigData.Screen,
    ): List<CustomerCenterConfigData.HelpPath> {
        return purchaseInformation?.let { info ->
            if (info.isLifetime) {
                screen.supportedPaths.filter { it.type != CustomerCenterConfigData.HelpPath.PathType.CANCEL }
            } else {
                screen.supportedPaths
            }
        } ?: emptyList()
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
        context: Context,
        product: StoreProduct,
        promotionalOffer: CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer,
        originalPath: CustomerCenterConfigData.HelpPath,
    ): Boolean {
        val offerIdentifier = promotionalOffer.productMapping[product.id]
        val subscriptionOption = product.subscriptionOptions?.firstOrNull { option ->
            when (option) {
                is GoogleSubscriptionOption ->
                    option.tags.contains(SharedConstants.RC_CUSTOMER_CENTER_TAG) && option.offerId == offerIdentifier

                else -> false
            }
        }
        var loaded = false
        if (subscriptionOption != null) {
            _state.update {
                val currentState = _state.value
                if (currentState is CustomerCenterState.Success) {
                    val localization = currentState.customerCenterConfigData.localization
                    val pricingPhasesDescription = subscriptionOption.getLocalizedDescription(localization, locale)
                    loaded = true
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
        return loaded
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

    override fun dismissPromotionalOffer(
        context: Context,
        originalPath: CustomerCenterConfigData.HelpPath,
    ) {
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

    override fun onNavigationButtonPressed(context: Context, onDismiss: () -> Unit) {
        val currentState = _state.value
        // Handle special case for promotional offers first
        if (currentState is CustomerCenterState.Success && currentState.promotionalOfferData != null) {
            dismissPromotionalOffer(context, currentState.promotionalOfferData.originalPath)
            return
        }

        val navigationButtonType = state.value.navigationButtonType

        _state.update { state ->
            when {
                // For BACK button: Return to main screen without losing loaded data
                state is CustomerCenterState.Success &&
                    navigationButtonType == CustomerCenterState.NavigationButtonType.BACK ->
                    state.resetToMainScreen()
                // For all other cases (including CLOSE): Reset to NotLoaded
                else -> CustomerCenterState.NotLoaded
            }
        }

        // Call onDismiss only for the CLOSE button
        if (navigationButtonType == CustomerCenterState.NavigationButtonType.CLOSE) {
            onDismiss()
        }
    }

    override suspend fun loadCustomerCenter() {
        _state.update { state ->
            if (state !is CustomerCenterState.Loading) {
                CustomerCenterState.Loading
            } else {
                state
            }
        }
        try {
            val customerCenterConfigData = purchases.awaitCustomerCenterConfigData()
            val purchaseInformation = loadPurchaseInformation(dateFormatter, locale)
            _state.update {
                CustomerCenterState.Success(
                    customerCenterConfigData,
                    purchaseInformation,
                    supportedPathsForManagementScreen = customerCenterConfigData.getManagementScreen()?.let {
                        supportedPaths(purchaseInformation, it)
                    },
                )
            }
        } catch (e: PurchasesException) {
            _state.update {
                CustomerCenterState.Error(e.error)
            }
        }
    }

    override fun refreshStateIfLocaleChanged() {
        val currentLocaleList = getCurrentLocaleList()
        if (_lastLocaleList.value != currentLocaleList) {
            _lastLocaleList.value = currentLocaleList
        }
    }

    override fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDark: Boolean) {
        if (isDarkMode != isDark) {
            isDarkMode = isDark
        }

        if (_colorScheme.value != colorScheme) {
            _colorScheme.value = colorScheme
        }
    }

    override fun trackImpressionIfNeeded() {
        if (impressionCreationData == null) {
            impressionCreationData = CustomerCenterImpressionEvent.CreationData()

            val locale = _lastLocaleList.value.get(0) ?: Locale.getDefault()
            val event = CustomerCenterImpressionEvent(
                data = CustomerCenterImpressionEvent.Data(
                    timestamp = Date(),
                    darkMode = isDarkMode,
                    locale = locale.toString(),
                ),
            )
            purchases.track(event)
        }
    }

    private fun trackCustomerCenterEventOptionChosen(
        path: CustomerCenterConfigData.HelpPath.PathType,
        url: String?,
        surveyOptionID: String,
    ) {
        val locale = _lastLocaleList.value.get(0) ?: Locale.getDefault()
        val event = CustomerCenterSurveyOptionChosenEvent(
            data = CustomerCenterSurveyOptionChosenEvent.Data(
                timestamp = Date(),
                darkMode = isDarkMode,
                locale = locale.toString(),
                path = path,
                url = url,
                surveyOptionID = surveyOptionID,
            ),
        )
        purchases.track(event)
    }

    private fun getCurrentLocaleList(): LocaleListCompat {
        return LocaleListCompat.getDefault()
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
            when (currentState) {
                is CustomerCenterState.Success -> currentState.resetToMainScreen()
                else -> currentState
            }
        }
    }

    private fun CustomerCenterState.Success.resetToMainScreen() =
        copy(
            feedbackSurveyData = null,
            promotionalOfferData = null,
            showRestoreDialog = false,
            title = null,
            navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
        )

    private fun showManageSubscriptions(context: Context, productId: String) {
        try {
            val packageName = context.packageName
            val uri = "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            Logger.e("Error opening manage subscriptions", e)
        }
    }

    private fun notifyListenersForRestoreStarted() {
        listener?.onRestoreStarted()
        purchases.customerCenterListener?.onRestoreStarted()
    }

    private fun notifyListenersForRestoreCompleted(customerInfo: CustomerInfo) {
        listener?.onRestoreCompleted(customerInfo)
        purchases.customerCenterListener?.onRestoreCompleted(customerInfo)
    }

    private fun notifyListenersForRestoreFailed(error: PurchasesError) {
        listener?.onRestoreFailed(error)
        purchases.customerCenterListener?.onRestoreFailed(error)
    }

    private fun notifyListenersForManageSubscription() {
        listener?.onShowingManageSubscriptions()
        purchases.customerCenterListener?.onShowingManageSubscriptions()
    }

    private fun notifyListenersForFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
        listener?.onFeedbackSurveyCompleted(feedbackSurveyOptionId)
        purchases.customerCenterListener?.onFeedbackSurveyCompleted(feedbackSurveyOptionId)
    }
}
