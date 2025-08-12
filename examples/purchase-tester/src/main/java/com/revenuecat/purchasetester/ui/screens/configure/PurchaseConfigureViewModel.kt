package com.revenuecat.purchasetester.ui.screens.configure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.purchasetester.utils.DataStoreUtils
import com.revenuecat.purchasetester.MainApplication
import com.revenuecat.purchasetester.NotFinishingTransactionsBillingClient
import com.revenuecat.purchasetester.SdkConfiguration
import com.revenuecat.purchasetester.utils.configurationDataStore
import com.revenuecat.purchasetester.ui.model.configure.PurchaseConfigureState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL

interface PurchaseConfigureViewModel {
    val state: StateFlow<PurchaseConfigureState>
    fun onAction(action: PurchaseConfigureActions)
}

class PurchaseConfigureViewModelImpl(
    private val dataStoreUtils: DataStoreUtils,
    private val application: MainApplication
) : ViewModel(), PurchaseConfigureViewModel {

    private val _state = MutableStateFlow(PurchaseConfigureState())
    override val state: StateFlow<PurchaseConfigureState> = _state.asStateFlow()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                PurchaseConfigureViewModelImpl(
                    dataStoreUtils = DataStoreUtils(context.applicationContext.configurationDataStore),
                    application = context.applicationContext as MainApplication
                )
            }
        }
    }

    init {
        loadSavedConfiguration()
        setupSupportedStores()
    }

    private fun loadSavedConfiguration() {
        dataStoreUtils.getSdkConfig()
            .onEach { sdkConfiguration ->
                _state.value = _state.value.copy(
                    apiKey = sdkConfiguration.apiKey,
                    proxyUrl = sdkConfiguration.proxyUrl ?: "" ,
                    useAmazonStore = sdkConfiguration.useAmazon
                )
            }
            .launchIn(viewModelScope)
    }

    private fun setupSupportedStores() {
        val supportedStores = com.revenuecat.purchasetester.BuildConfig.SUPPORTED_STORES.split(",")
        _state.value = _state.value.copy(
            supportedStores = SupportedStores(
                googleSupported = supportedStores.contains("google"),
                amazonSupported = supportedStores.contains("amazon")
            )
        )
    }

    override fun onAction(action: PurchaseConfigureActions) {
        when (action) {
            is PurchaseConfigureActions.OnContinue -> onContinue()
            is PurchaseConfigureActions.OnApiKeyChanged -> onApiKeyChanged(action.apiKey)
            is PurchaseConfigureActions.OnProxyUrlChanged -> onProxyUrlChanged(action.proxyUrl)
            is PurchaseConfigureActions.OnEntitlementVerificationModeChanged -> onEntitlementVerificationModeChanged(action.mode)
            is PurchaseConfigureActions.OnStoreChanged -> onStoreChanged(action.useAmazon)
            is PurchaseConfigureActions.OnPurchaseCompletionChanged -> onPurchaseCompletionChanged(action.completedBy)
            is PurchaseConfigureActions.OnErrorDismissed -> onErrorDismissed()
        }
    }

    private fun onApiKeyChanged(apiKey: String) {
        _state.value = _state.value.copy(apiKey = apiKey)
    }

    private fun onProxyUrlChanged(proxyUrl: String) {
        _state.value = _state.value.copy(proxyUrl = proxyUrl)

        // Update proxy URL in real-time like in the original fragment
        try {
            if (proxyUrl.isNotEmpty()) {
                val url = URL(proxyUrl)
                Purchases.proxyURL = url
            } else {
                Purchases.proxyURL = null
            }
        } catch (e: MalformedURLException) {
            Purchases.proxyURL = null
        }
    }

    private fun onEntitlementVerificationModeChanged(mode: EntitlementVerificationMode) {
        _state.value = _state.value.copy(entitlementVerificationMode = mode)
    }

    private fun onStoreChanged(useAmazon: Boolean) {
        _state.value = _state.value.copy(
            useAmazonStore = useAmazon,
            // Force RevenueCat completion for Amazon like in original fragment
            purchasesAreCompletedBy = if (useAmazon) PurchasesAreCompletedBy.REVENUECAT else _state.value.purchasesAreCompletedBy
        )
    }

    private fun onPurchaseCompletionChanged(completedBy: PurchasesAreCompletedBy) {
        // Don't allow changing if Amazon is selected
        if (!_state.value.useAmazonStore) {
            _state.value = _state.value.copy(purchasesAreCompletedBy = completedBy)
        }
    }

    private fun onErrorDismissed() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun onContinue() {
        if (!validateInputs()) return

        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                configureSDK()
                _state.value = _state.value.copy(
                    isLoading = false,
                    isConfigured = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to configure SDK: ${e.message}"
                )
            }
        }
    }

    private fun onNavigateToLogs() {
        // Navigation will be handled by the composable/fragment
        // This is just a placeholder for the action
    }

    private fun onNavigateToProxy() {
        // Navigation will be handled by the composable/fragment
        // This is just a placeholder for the action
    }

    private suspend fun configureSDK() {
        val currentState = _state.value

        if (currentState.proxyUrl.isNotEmpty()) {
            Purchases.proxyURL = URL(currentState.proxyUrl)
        }

        Purchases.logLevel = LogLevel.VERBOSE

        val configurationBuilder = if (currentState.useAmazonStore) {
            AmazonConfiguration.Builder(application, currentState.apiKey)
        } else {
            PurchasesConfiguration.Builder(application, currentState.apiKey)
        }

        val configuration = configurationBuilder
            .diagnosticsEnabled(true)
            .entitlementVerificationMode(currentState.entitlementVerificationMode)
            .purchasesAreCompletedBy(currentState.purchasesAreCompletedBy)
            .pendingTransactionsForPrepaidPlansEnabled(true)
            .build()

        Purchases.configure(configuration)

        if (currentState.purchasesAreCompletedBy == PurchasesAreCompletedBy.MY_APP) {
            NotFinishingTransactionsBillingClient.start(application, application.logHandler)
        }

        // Set attributes to store additional, structured information for a user in RevenueCat.
        // More info: https://docs.revenuecat.com/docs/user-attributes
        Purchases.sharedInstance.setAttributes(mapOf("favorite_cat" to "garfield"))

        Purchases.sharedInstance.updatedCustomerInfoListener = application

        dataStoreUtils.saveSdkConfig(
            SdkConfiguration(
                apiKey = currentState.apiKey,
                proxyUrl = currentState.proxyUrl,
                useAmazon = currentState.useAmazonStore
            )
        )
    }

    private fun validateInputs(): Boolean {
        return validateApiKey() && validateProxyURL()
    }

    private fun validateApiKey(): Boolean {
        val apiKey = _state.value.apiKey
        if (apiKey.isEmpty()) {
            _state.value = _state.value.copy(errorMessage = "API Key is empty")
            return false
        }
        return true
    }

    private fun validateProxyURL(): Boolean {
        val proxyUrl = _state.value.proxyUrl
        if (proxyUrl.isEmpty()) return true

        try {
            URL(proxyUrl)
        } catch (e: MalformedURLException) {
            _state.value = _state.value.copy(errorMessage = "Invalid proxy URL. Could not convert to URL.")
            return false
        }
        return true
    }
}