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
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.common.SharedConstants
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import com.revenuecat.purchases.customercenter.events.CustomerCenterImpressionEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterSurveyOptionChosenEvent
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.Transaction
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PathUtils
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.extensions.getLocalizedDescription
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
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
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion as CrossProductPromotion

@Suppress("TooManyFunctions")
internal interface CustomerCenterViewModel {
    val state: StateFlow<CustomerCenterState>
    val actionError: State<PurchasesError?>

    fun pathButtonPressed(
        context: Context,
        path: HelpPath,
        product: PurchaseInformation?,
    )

    fun selectPurchase(purchase: PurchaseInformation)

    suspend fun dismissRestoreDialog()
    suspend fun restorePurchases()
    fun contactSupport(context: Context, supportEmail: String)
    suspend fun loadAndDisplayPromotionalOffer(
        context: Context,
        product: StoreProduct,
        promotionalOffer: HelpPath.PathDetail.PromotionalOffer,
        originalPath: HelpPath,
        purchaseInformation: PurchaseInformation? = null,
    ): Boolean

    suspend fun onAcceptedPromotionalOffer(subscriptionOption: SubscriptionOption, activity: Activity?)
    fun dismissPromotionalOffer(context: Context, originalPath: HelpPath)
    fun onNavigationButtonPressed(context: Context, onDismiss: () -> Unit)
    suspend fun loadCustomerCenter()
    fun openURL(
        context: Context,
        url: String,
        method: HelpPath.OpenMethod = HelpPath.OpenMethod.EXTERNAL,
    )

    fun clearActionError()

fun onCustomActionSelected(customActionData: CustomActionData)

    // trigger state refresh
    fun refreshStateIfLocaleChanged()
    fun refreshStateIfColorsChanged(currentColorScheme: ColorScheme, isSystemInDarkTheme: Boolean)

    // tracks customer center impression the first time is shown
    fun trackImpressionIfNeeded()
}

internal sealed class TransactionDetails(
    open val productIdentifier: String,
    open val store: Store,
) {
    data class Subscription(
        override val productIdentifier: String,
        val productPlanIdentifier: String?,
        override val store: Store,
        val isActive: Boolean,
        val willRenew: Boolean,
        val expiresDate: Date?,
        val isTrial: Boolean,
        val managementURL: Uri?,
    ) : TransactionDetails(productIdentifier, store)

    data class NonSubscription(
        override val productIdentifier: String,
        override val store: Store,
    ) : TransactionDetails(productIdentifier, store)
}

@Suppress("TooManyFunctions", "LargeClass")
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

    override fun pathButtonPressed(
        context: Context,
        path: HelpPath,
        purchaseInformation: PurchaseInformation?,
    ) {
        notifyListenersForManagementOptionSelected(path)
        path.feedbackSurvey?.let { feedbackSurvey ->
            displayFeedbackSurvey(feedbackSurvey, onAnswerSubmitted = { option ->
                option?.let {
                    trackCustomerCenterEventOptionChosen(
                        path = path.type,
                        url = path.url,
                        surveyOptionID = it.id,
                    )
                    notifyListenersForFeedbackSurveyCompleted(it.id)

                    viewModelScope.launch {
                        val promotionalOfferDisplayed =
                            handlePromotionalOffer(
                                context,
                                purchaseInformation?.product,
                                it.promotionalOffer,
                                path,
                                purchaseInformation,
                            )
                        if (!promotionalOfferDisplayed) {
                            // No promotional offer, close survey and execute main path action
                            goBackToMain()
                            mainPathAction(path, context, purchaseInformation)
                        }
                    }
                } ?: run {
                    goBackToMain()
                }
            })
            return
        }
        viewModelScope.launch {
            val promotionalOfferDisplayed =
                handlePromotionalOffer(
                    context,
                    purchaseInformation?.product,
                    path.promotionalOffer,
                    path,
                    purchaseInformation,
                )
            if (!promotionalOfferDisplayed) {
                mainPathAction(path, context, purchaseInformation)
            }
        }
    }

    override fun selectPurchase(purchase: PurchaseInformation) {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                val screen = currentState.customerCenterConfigData.getManagementScreen()
                if (screen != null) {
                    val baseSupportedPaths = supportedPaths(
                        purchase,
                        screen,
                        currentState.customerCenterConfigData.localization,
                    )

                    // For detail screen: only show subscription-specific actions
                    val detailSupportedPaths = PathUtils.filterSubscriptionSpecificPaths(baseSupportedPaths)

                    currentState.copy(
                        navigationState = currentState.navigationState.push(
                            CustomerCenterDestination.SelectedPurchaseDetail(purchase, screen.title),
                        ),
                        navigationButtonType = CustomerCenterState.NavigationButtonType.BACK,
                        detailScreenPaths = detailSupportedPaths,
                    )
                } else {
                    Logger.e("No management screen available in the customer center config data")
                    CustomerCenterState.Error(
                        PurchasesError(
                            PurchasesErrorCode.UnknownError,
                            "No management screen available in the customer center config data",
                        ),
                    )
                }
            } else {
                currentState
            }
        }
    }

    private fun handleCancelPath(context: Context, purchaseInformation: PurchaseInformation? = null) {
        val currentState = _state.value as? CustomerCenterState.Success ?: return
        val purchaseInfo = purchaseInformation ?: when (val destination = currentState.currentDestination) {
            is CustomerCenterDestination.SelectedPurchaseDetail -> destination.purchaseInformation
            else -> {
                // If we're on the main screen and there's only one purchase, use that purchase
                if (currentState.purchases.size == 1) {
                    currentState.purchases.first()
                } else {
                    null
                }
            }
        }

        when {
            purchaseInfo?.store == Store.PLAY_STORE && purchaseInfo.product != null ->
                startGoogleProductCancellation(context, purchaseInfo.product.id)
            purchaseInfo?.managementURL != null -> startManagementUrlCancellation(context, purchaseInfo.managementURL)
            else -> Logger.e("No product or management URL available for cancel path")
        }
    }

    private fun startGoogleProductCancellation(context: Context, productId: String) {
        notifyListenersForManageSubscription()
        showManageSubscriptions(context, productId)
    }

    private fun startManagementUrlCancellation(context: Context, managementURL: Uri) {
        notifyListenersForManageSubscription()
        openURL(
            context,
            managementURL.toString(),
            HelpPath.OpenMethod.EXTERNAL,
        )
    }

    private fun mainPathAction(
        path: HelpPath,
        context: Context,
        purchaseInformation: PurchaseInformation? = null,
    ) {
        when (path.type) {
            HelpPath.PathType.MISSING_PURCHASE -> {
                _state.update { currentState ->
                    when (currentState) {
                        is CustomerCenterState.Success -> {
                            currentState.copy(restorePurchasesState = RestorePurchasesState.RESTORING)
                        }
                        else -> currentState
                    }
                }
            }

            HelpPath.PathType.CANCEL -> handleCancelPath(context, purchaseInformation)

            HelpPath.PathType.CUSTOM_URL -> {
                path.url?.let {
                    openURL(
                        context,
                        it,
                        path.openMethod ?: HelpPath.OpenMethod.EXTERNAL,
                    )
                }
            }

            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION -> {
                path.actionIdentifier?.let { actionIdentifier ->
                    val customActionData = com.revenuecat.purchases.customercenter.CustomActionData(
                        actionIdentifier = actionIdentifier,
                        purchaseIdentifier = purchaseInformation?.product?.id,
                    )
                    onCustomActionSelected(customActionData)
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
        selectedPurchaseInformation: PurchaseInformation?,
        screen: CustomerCenterConfigData.Screen,
        localization: CustomerCenterConfigData.Localization,
    ): List<HelpPath> {
        return screen.paths
            .filter { isPathAllowedForStore(it, selectedPurchaseInformation) }
            .filter { isPathAllowedForSubscriptionState(it, selectedPurchaseInformation) }
            .transformPathsOnSubscriptionState(selectedPurchaseInformation, localization)
    }

    private fun List<HelpPath>.transformPathsOnSubscriptionState(
        selectedPurchaseInformation: PurchaseInformation?,
        localization: CustomerCenterConfigData.Localization,
    ): List<HelpPath> {
        return map { path ->
            // For cancelled subscriptions, show "Resubscribe" instead of "Cancel"
            if (path.type == HelpPath.PathType.CANCEL &&
                selectedPurchaseInformation?.isCancelled == true
            ) {
                path.copy(
                    title = localization.commonLocalizedString(
                        CustomerCenterConfigData.Localization.CommonLocalizedString.RESUBSCRIBE,
                    ),
                    feedbackSurvey = null,
                    promotionalOffer = null,
                )
            } else {
                path
            }
        }
    }

    private fun isPathAllowedForSubscriptionState(
        path: HelpPath,
        purchaseInformation: PurchaseInformation?,
    ): Boolean {
        if (path.type == HelpPath.PathType.CANCEL) {
            return purchaseInformation?.isSubscription == true && !purchaseInformation.isExpired
        }
        return true
    }

    private fun isPathAllowedForStore(
        path: HelpPath,
        purchaseInformation: PurchaseInformation?,
    ): Boolean {
        return when (path.type) {
            HelpPath.PathType.MISSING_PURCHASE,
            HelpPath.PathType.CUSTOM_URL,
            HelpPath.PathType.CUSTOM_ACTION,
            -> true
            HelpPath.PathType.CANCEL ->
                purchaseInformation?.store == Store.PLAY_STORE || purchaseInformation?.managementURL != null
            HelpPath.PathType.REFUND_REQUEST,
            HelpPath.PathType.CHANGE_PLANS,
            HelpPath.PathType.UNKNOWN,
            -> false
        }
    }

    private fun computeMainScreenPaths(state: CustomerCenterState.Success): List<HelpPath> {
        val screenToUse = if (state.purchases.isNotEmpty() && state.purchases.any { !it.isExpired }) {
            state.customerCenterConfigData.getManagementScreen()
        } else {
            state.customerCenterConfigData.getNoActiveScreen()
        }

        val baseSupportedPaths = screenToUse?.let { screen ->
            val selectedPurchase = if (state.purchases.size == 1) {
                state.purchases.first()
            } else {
                null
            }
            supportedPaths(selectedPurchase, screen, state.customerCenterConfigData.localization)
        } ?: emptyList()

        // For main screen: if multiple purchases, show only general paths
        // If single purchase or no purchases, show all available paths
        return if (state.purchases.size > 1) {
            PathUtils.filterGeneralPaths(baseSupportedPaths)
        } else {
            baseSupportedPaths
        }
    }

    private suspend fun loadPurchases(
        dateFormatter: DateFormatter,
        locale: Locale,
    ): List<PurchaseInformation> {
        val customerInfo = purchases.awaitCustomerInfo(fetchPolicy = CacheFetchPolicy.FETCH_CURRENT)

        val hasActiveSubscriptions = customerInfo.activeSubscriptions.isNotEmpty()
        val hasNonSubscriptionTransactions = customerInfo.nonSubscriptionTransactions.isNotEmpty()

        if (hasActiveSubscriptions || hasNonSubscriptionTransactions) {
            val activeTransactions = findActiveTransactions(customerInfo)

            if (activeTransactions.isNotEmpty()) {
                return activeTransactions.map { transaction ->
                    val entitlement = customerInfo.entitlements.all.values
                        .firstOrNull { it.productIdentifier == transaction.productIdentifier }

                    createPurchaseInformation(
                        transaction,
                        entitlement,
                        dateFormatter,
                        locale,
                    )
                }
            } else {
                Logger.w("Could not find subscription information")
            }
        }

        // If no active purchases found, try to find the latest expired subscription
        val latestExpiredTransaction = findLatestExpiredSubscription(customerInfo)
        return if (latestExpiredTransaction != null) {
            val entitlement = customerInfo.entitlements.all.values
                .firstOrNull { it.productIdentifier == latestExpiredTransaction.productIdentifier }

            listOf(
                createPurchaseInformation(
                    latestExpiredTransaction,
                    entitlement,
                    dateFormatter,
                    locale,
                ),
            )
        } else {
            emptyList()
        }
    }

    private fun findActiveTransactions(customerInfo: CustomerInfo): List<TransactionDetails> {
        val activeSubscriptions = customerInfo.subscriptionsByProductIdentifier.values
            .filter { it.isActive }
            .sortedBy { it.expiresDate }

        val activeGoogleSubscriptions = activeSubscriptions.filter { it.store == Store.PLAY_STORE }
        val googleNonSubscriptions = customerInfo.nonSubscriptionTransactions.filter { it.store == Store.PLAY_STORE }
        val otherActiveSubscriptions = activeSubscriptions.filter { it.store != Store.PLAY_STORE }
        val otherNonSubscriptions = customerInfo.nonSubscriptionTransactions.filter { it.store != Store.PLAY_STORE }

        val prioritized =
            activeGoogleSubscriptions + googleNonSubscriptions + otherActiveSubscriptions + otherNonSubscriptions

        return prioritized.mapNotNull {
            when (it) {
                is SubscriptionInfo -> it.asTransactionDetails()

                is Transaction -> TransactionDetails.NonSubscription(
                    productIdentifier = it.productIdentifier,
                    store = it.store,
                )

                else -> null
            }
        }
    }

    private fun findLatestExpiredSubscription(customerInfo: CustomerInfo): TransactionDetails.Subscription? {
        return customerInfo.subscriptionsByProductIdentifier.values
            .filter { !it.isActive && it.expiresDate != null }
            .maxByOrNull { it.expiresDate!! }?.asTransactionDetails()
    }

    private suspend fun createPurchaseInformation(
        transaction: TransactionDetails,
        entitlement: EntitlementInfo?,
        dateFormatter: DateFormatter,
        locale: Locale,
    ): PurchaseInformation {
        val product = if (transaction.store == Store.PLAY_STORE) {
            purchases.awaitGetProduct(
                transaction.productIdentifier,
                (transaction as? TransactionDetails.Subscription)?.productPlanIdentifier,
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
            dateFormatter = dateFormatter,
            locale = locale,
        )
    }

    override fun contactSupport(context: Context, supportEmail: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:$supportEmail".toUri()
            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            putExtra(Intent.EXTRA_TEXT, "Support request details...")
        }
        context.startActivity(Intent.createChooser(intent, "Contact Support"))
    }

    @SuppressWarnings("ForbiddenComment")
    override fun openURL(context: Context, url: String, method: HelpPath.OpenMethod) {
        val openingMethod = when (method) {
            HelpPath.OpenMethod.IN_APP -> URLOpeningMethod.IN_APP_BROWSER
            HelpPath.OpenMethod.EXTERNAL,
            -> URLOpeningMethod.EXTERNAL_BROWSER
        }
        URLOpener.openURL(context, url, openingMethod)
    }

    override fun clearActionError() {
        _actionError.value = null
    }

    @SuppressWarnings("ReturnCount")
    override suspend fun loadAndDisplayPromotionalOffer(
        context: Context,
        product: StoreProduct,
        promotionalOffer: HelpPath.PathDetail.PromotionalOffer,
        originalPath: HelpPath,
        purchaseInformation: PurchaseInformation?,
    ): Boolean {
        if (!promotionalOffer.eligible) {
            Logger.d(
                "User not eligible for promo with id '${promotionalOffer.androidOfferId}'. " +
                    "Check eligibility configuration in the dashboard, and make sure the user has " +
                    "an active/expired subscription for the product with id '${product.id}'.",
            )
            return false
        }

        val subscriptionOption = getPromotionalSubscriptionOption(promotionalOffer, product) ?: return false

        var loaded = false
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                val localization = currentState.customerCenterConfigData.localization
                val pricingPhasesDescription = subscriptionOption.getLocalizedDescription(localization, locale)
                loaded = true
                val promotionalOfferDestination = CustomerCenterDestination.PromotionalOffer(
                    data = PromotionalOfferData(
                        promotionalOffer,
                        subscriptionOption,
                        originalPath,
                        pricingPhasesDescription,
                    ),
                    purchaseInformation = purchaseInformation,
                )
                currentState.copy(
                    navigationState = currentState.navigationState.push(promotionalOfferDestination),
                    navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
                )
                currentState.copy(
                    navigationState = currentState.navigationState.push(promotionalOfferDestination),
                    navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
                )
            } else {
                currentState
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

            // Reload customer center data to refresh the UI with the latest subscription information
            // It will also go back to main screen
            loadCustomerCenter()
        } catch (e: PurchasesException) {
            if (e.code != PurchasesErrorCode.PurchaseCancelledError) {
                _actionError.value = e.error
                goBackToMain()
            }
        }
    }

    override fun dismissPromotionalOffer(
        context: Context,
        originalPath: HelpPath,
    ) {
        val purchaseInfo = (_state.value as? CustomerCenterState.Success).let { currentState ->
            when (val destination = currentState?.currentDestination) {
                is CustomerCenterDestination.PromotionalOffer -> destination.purchaseInformation
                else -> null
            }
        }

        // Continue with the original action and remove the promotional offer data
        mainPathAction(originalPath, context, purchaseInfo)

        _state.update { state ->
            if (state is CustomerCenterState.Success) {
                state.copy(
                    navigationState = state.navigationState.popToMain(),
                    navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
                )
            } else {
                state
            }
        }
    }

    override fun onNavigationButtonPressed(context: Context, onDismiss: () -> Unit) {
        val currentState = _state.value
        // Handle special case for promotional offers first
        if (currentState is CustomerCenterState.Success &&
            currentState.currentDestination is CustomerCenterDestination.PromotionalOffer
        ) {
            dismissPromotionalOffer(
                context,
                (currentState.currentDestination as CustomerCenterDestination.PromotionalOffer).data.originalPath,
            )
            return
        }

        val navigationButtonType = state.value.navigationButtonType

        _state.update { state ->
            when {
                // For BACK button: Navigate back in the stack
                state is CustomerCenterState.Success &&
                    navigationButtonType == CustomerCenterState.NavigationButtonType.BACK -> {
                    if (state.navigationState.canNavigateBack) {
                        val newNavigationState = state.navigationState.pop()

                        state.copy(
                            navigationState = newNavigationState,
                            navigationButtonType = if (newNavigationState.canNavigateBack) {
                                CustomerCenterState.NavigationButtonType.BACK
                            } else {
                                CustomerCenterState.NavigationButtonType.CLOSE
                            },
                        )
                    } else {
                        CustomerCenterState.NotLoaded
                    }
                }
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
            val purchases = loadPurchases(dateFormatter, locale)
            val successState = CustomerCenterState.Success(
                customerCenterConfigData,
                purchases,
                mainScreenPaths = emptyList(), // Will be computed below
                detailScreenPaths = emptyList(), // Will be computed when a purchase is selected
            )
            val mainScreenPaths = computeMainScreenPaths(successState)

            _state.update {
                successState.copy(mainScreenPaths = mainScreenPaths)
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

    override fun refreshStateIfColorsChanged(currentColorScheme: ColorScheme, isSystemInDarkTheme: Boolean) {
        if (isDarkMode != isSystemInDarkTheme) {
            isDarkMode = isSystemInDarkTheme
        }

        if (_colorScheme.value != currentColorScheme) {
            _colorScheme.value = currentColorScheme
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
        path: HelpPath.PathType,
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
        feedbackSurvey: HelpPath.PathDetail.FeedbackSurvey,
        onAnswerSubmitted: (HelpPath.PathDetail.FeedbackSurvey.Option?) -> Unit,
    ) {
        _state.update { currentState ->
            if (currentState is CustomerCenterState.Success) {
                val feedbackSurveyDestination = CustomerCenterDestination.FeedbackSurvey(
                    data = FeedbackSurveyData(feedbackSurvey, onAnswerSubmitted),
                    title = feedbackSurvey.title,
                )
                currentState.copy(
                    navigationState = currentState.navigationState.push(feedbackSurveyDestination),
                    navigationButtonType = CustomerCenterState.NavigationButtonType.BACK,
                )
            } else {
                currentState
            }
        }
    }

    @SuppressWarnings("ReturnCount")
    private suspend fun getPromotionalSubscriptionOption(
        promotionalOffer: HelpPath.PathDetail.PromotionalOffer,
        product: StoreProduct,
    ): SubscriptionOption? {
        val googleProduct = product.googleProduct
        val crossProductPromotion: CrossProductPromotion? =
            promotionalOffer.crossProductPromotions[product.id]
                // Check for cross-product promotions first (product.id includes base plan ex: "sub:p1m")
                // But it's possible the product is not configured with base plan in RevenueCat
                // so we also check for the Google product ID (which does not include base plan ex: "sub")
                ?: googleProduct?.let { promotionalOffer.crossProductPromotions[it.productId] }
                ?: promotionalOffer.productMapping[product.id]?.let {
                    CrossProductPromotion(
                        storeOfferIdentifier = it,
                        targetProductId = product.id,
                    )
                }

        if (crossProductPromotion == null) {
            Logger.d(
                "No promotional offer configured for product ${product.id}",
            )
            return null
        }

        val targetProduct: StoreProduct? = when {
            crossProductPromotion.targetProductId == product.id -> product
            googleProduct?.basePlanId != null ->
                // Passing original product's base plan ID to find the target product
                // in case the target product is missing the base plan ID in the dashboard
                // which is common for old products. Purchases.getProducts would return all products
                // with the same product ID but different base plan IDs. That way we can find the most relevant product.
                findTargetProduct(crossProductPromotion.targetProductId, googleProduct.basePlanId!!)
            else -> null
        }

        if (targetProduct == null) {
            Logger.d(
                "Could not find discount of product (${crossProductPromotion.targetProductId}) " +
                    "for active subscription ${product.id}",
            )
            return null
        }

        return getCustomerCenterSubscriptionOption(
            crossProductPromotion.storeOfferIdentifier,
            targetProduct,
        )
    }

    private suspend fun findTargetProduct(
        targetProductId: String,
        sourceBasePlan: String,
    ): StoreProduct? {
        val splitProduct = targetProductId.split(":")
        val productId = splitProduct.first()
        val basePlan = splitProduct.getOrNull(1) ?: sourceBasePlan
        val targetProduct = purchases.awaitGetProduct(productId, basePlan)
        return targetProduct
    }

    private fun getCustomerCenterSubscriptionOption(
        offerIdentifier: String,
        targetProduct: StoreProduct,
    ): SubscriptionOption? {
        return targetProduct.subscriptionOptions?.firstOrNull { option ->
            when (option) {
                is GoogleSubscriptionOption ->
                    option.tags.contains(SharedConstants.RC_CUSTOMER_CENTER_TAG) && option.offerId == offerIdentifier

                else -> false
            }
        }
    }

    private suspend fun handlePromotionalOffer(
        context: Context,
        product: StoreProduct?,
        promotionalOffer: HelpPath.PathDetail.PromotionalOffer?,
        path: HelpPath,
        purchaseInformation: PurchaseInformation?,
    ): Boolean {
        if (product != null && promotionalOffer != null) {
            return loadAndDisplayPromotionalOffer(
                context,
                product,
                promotionalOffer,
                path,
                purchaseInformation,
            )
        } else {
            return false
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
            navigationState = navigationState.popToMain(),
            restorePurchasesState = null,
            navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
        )

    private fun showManageSubscriptions(context: Context, productId: String) {
        try {
            val packageName = context.packageName
            val uri = "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
            context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
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

    private fun notifyListenersForManagementOptionSelected(path: HelpPath) {
        val action = when (path.type) {
            HelpPath.PathType.MISSING_PURCHASE ->
                CustomerCenterManagementOption.MissingPurchase

            HelpPath.PathType.CANCEL ->
                CustomerCenterManagementOption.Cancel

            HelpPath.PathType.CUSTOM_URL ->
                path.url?.let {
                    CustomerCenterManagementOption.CustomUrl(it.toUri())
                }

            CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION ->
                path.actionIdentifier?.let { actionIdentifier ->
                    CustomerCenterManagementOption.CustomAction(
                        actionIdentifier = actionIdentifier,
                        purchaseIdentifier = null, // This will be set appropriately when called from the UI
                    )
                }

            else -> null
        }
        if (action != null) {
            listener?.onManagementOptionSelected(action)
            purchases.customerCenterListener?.onManagementOptionSelected(action)
        }
    }

    override fun onCustomActionSelected(customActionData: com.revenuecat.purchases.customercenter.CustomActionData) {
        notifyListenersForCustomActionSelected(customActionData)
    }

    private fun notifyListenersForCustomActionSelected(
        customActionData: com.revenuecat.purchases.customercenter.CustomActionData,
    ) {
        listener?.onCustomActionSelected(customActionData.actionIdentifier, customActionData.purchaseIdentifier)
        purchases.customerCenterListener?.onCustomActionSelected(
            customActionData.actionIdentifier,
            customActionData.purchaseIdentifier,
        )
    }

    private fun SubscriptionInfo.asTransactionDetails() = TransactionDetails.Subscription(
        productIdentifier = productIdentifier,
        productPlanIdentifier = productPlanIdentifier,
        store = store,
        isActive = isActive,
        willRenew = willRenew,
        expiresDate = expiresDate,
        isTrial = periodType == PeriodType.TRIAL,
        managementURL = managementURL,
    )
}
