package com.revenuecat.purchasetester.ui.screens.configure

import android.util.Patterns
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
import com.revenuecat.purchasetester.DataStoreUtils
import com.revenuecat.purchasetester.MainApplication
import com.revenuecat.purchasetester.NotFinishingTransactionsBillingClient
import com.revenuecat.purchasetester.SdkConfiguration
import com.revenuecat.purchasetester.configurationDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URL

interface ConfigureScreenViewModel {

    val state: StateFlow<ConfigureScreenState>
    val events: SharedFlow<ConfigureUiEvent>

    fun validateInputs(apiKey: String, proxyUrl: String): ValidationResult
    fun configureSDK(application: MainApplication)
    fun saveApiKey(apiKey: String)
    fun saveProxyUrl(proxyUrl: String)
    fun saveEntitlementVerificationMode(entitlementVerificationMode: EntitlementVerificationMode)
    fun saveStoreType(storeType: StoreType)
    fun savePurchasesAreCompletedBy(purchasesAreCompletedBy: PurchasesAreCompletedBy)
}

class ConfigureScreenViewModelImpl(
    private val dataStoreUtils: DataStoreUtils,
) : ViewModel(), ConfigureScreenViewModel {

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5000L
        
        val Factory = viewModelFactory {
            initializer {
                val context = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                ConfigureScreenViewModelImpl(
                    dataStoreUtils = DataStoreUtils(context.configurationDataStore),
                )
            }
        }
    }

    override val events: SharedFlow<ConfigureUiEvent>
        get() = _events.asSharedFlow()

    private val _events = MutableSharedFlow<ConfigureUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    private val userEdits = MutableStateFlow<ConfigureScreenState.ConfigureScreenData?>(null)

    private val _state: StateFlow<ConfigureScreenState> = combine(
        dataStoreUtils.getSdkConfig(),
        userEdits
    ) { sdkConfiguration, edits ->
        edits ?: ConfigureScreenState.ConfigureScreenData(
            apiKey = sdkConfiguration.apiKey,
            proxyUrl = sdkConfiguration.proxyUrl.orEmpty(),
            selectedStoreType = if (sdkConfiguration.useAmazon) {
                StoreType.AMAZON
            } else {
                StoreType.GOOGLE
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = ConfigureScreenState.Loading
    )

    override val state: StateFlow<ConfigureScreenState>
        get() = _state

    override fun validateInputs(apiKey: String, proxyUrl: String): ValidationResult {
        if (apiKey.isBlank()) {
            return ValidationResult.Invalid("API Key cannot be empty")
        }

        if (proxyUrl.isNotBlank()) {
            runCatching {
                URL(proxyUrl)
            }.onFailure {
                return ValidationResult.Invalid("Invalid proxy URL format")
            }
        }

        return ValidationResult.Valid
    }

    private fun emitEvent(event: ConfigureUiEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    override fun configureSDK(application: MainApplication) {
        val currentState = state.value
        if (currentState !is ConfigureScreenState.ConfigureScreenData) {
            emitEvent(ConfigureUiEvent.Error("Configuration state not ready"))
            return
        }

        viewModelScope.launch {
            runCatching {
                configurePurchasesSDK(application, currentState)
                persistConfiguration(currentState)
            }.onSuccess {
                emitEvent(ConfigureUiEvent.Success)
            }.onFailure { throwable ->
                val message = when (throwable) {
                    is IllegalStateException -> "SDK configuration failed: ${throwable.message}"
                    else -> throwable.message ?: "Failed to configure SDK"
                }
                emitEvent(ConfigureUiEvent.Error(message))
            }
        }
    }

    private fun configurePurchasesSDK(
        application: MainApplication,
        currentState: ConfigureScreenState.ConfigureScreenData,
    ) {
        configureProxyUrl(currentState.proxyUrl.trim())

        Purchases.logLevel = LogLevel.VERBOSE

        val useAmazonStore = currentState.selectedStoreType == StoreType.AMAZON

        val configurationBuilder = if (useAmazonStore) {
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

        // set attributes to store additional, structured information for a user in RevenueCat.
        // More info: https://docs.revenuecat.com/docs/user-attributes
        Purchases.sharedInstance.setAttributes(mapOf("favorite_cat" to "garfield"))

        Purchases.sharedInstance.updatedCustomerInfoListener = application
    }

    private suspend fun persistConfiguration(currentState: ConfigureScreenState.ConfigureScreenData) {
        val useAmazonStore = currentState.selectedStoreType == StoreType.AMAZON
        dataStoreUtils.saveSdkConfig(
            SdkConfiguration(
                currentState.apiKey,
                currentState.proxyUrl,
                useAmazonStore
            )
        )
    }

    override fun saveApiKey(apiKey: String) {
        updateData { copy(apiKey = apiKey.trim()) }
    }

    override fun saveProxyUrl(proxyUrl: String) {
        val trimmedUrl = proxyUrl.trim()
        updateData { copy(proxyUrl = trimmedUrl) }
        configureProxyUrl(trimmedUrl)
    }

    override fun saveEntitlementVerificationMode(entitlementVerificationMode: EntitlementVerificationMode) {
        updateData { copy(entitlementVerificationMode = entitlementVerificationMode) }
    }

    override fun saveStoreType(storeType: StoreType) {
        updateData { copy(selectedStoreType = storeType) }
    }

    override fun savePurchasesAreCompletedBy(purchasesAreCompletedBy: PurchasesAreCompletedBy) {
        updateData { copy(purchasesAreCompletedBy = purchasesAreCompletedBy) }
    }

    private inline fun updateData(
        block: ConfigureScreenState.ConfigureScreenData.() -> ConfigureScreenState.ConfigureScreenData,
    ) {
        val current = _state.value
        if (current is ConfigureScreenState.ConfigureScreenData) {
            userEdits.value = block(current)
        }
    }

    private fun configureProxyUrl(proxyUrl: String) {
        try {
            if (proxyUrl.isUrl()) {
                Purchases.proxyURL = URL(proxyUrl)
            } else {
                Purchases.proxyURL = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun String.isUrl(): Boolean {
        if (this.isBlank()) return false
        return Patterns.WEB_URL.matcher(this).matches()
    }

}
