package com.revenuecat.paywallstester.ui.screens.main.appinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.paywallstester.ConfigurePurchasesUseCase
import com.revenuecat.paywallstester.Constants
import com.revenuecat.paywallstester.data.ApiKeyStore
import com.revenuecat.paywallstester.data.SubscriberAttributesStore
import com.revenuecat.paywallstester.ui.screens.main.appinfo.AppInfoScreenViewModel.UiState
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface AppInfoScreenViewModel {
    data class UiState(
        val appUserID: String,
        val apiKeyDescription: String,
        val activeEntitlements: List<String>,
        val subscriberAttributes: Map<String, String> = emptyMap(),
    ) {
        companion object {
            val Empty = UiState(
                appUserID = "",
                apiKeyDescription = "",
                activeEntitlements = emptyList(),
            )
        }
    }

    val state: StateFlow<UiState>

    fun logIn(newAppUserId: String)
    fun logOut()
    fun switchApiKey(newApiKey: String)
    fun refresh()
    fun setSubscriberAttribute(key: String, value: String)
    fun clearSubscriberAttribute(key: String)
    fun clearAllSubscriberAttributes()
}

internal class AppInfoScreenViewModelImpl(
    private val configurePurchases: ConfigurePurchasesUseCase,
    private val apiKeyStore: ApiKeyStore,
    private val subscriberAttributesStore: SubscriberAttributesStore,
) : ViewModel(), AppInfoScreenViewModel {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                AppInfoScreenViewModelImpl(
                    configurePurchases = ConfigurePurchasesUseCase(context),
                    apiKeyStore = ApiKeyStore(context),
                    subscriberAttributesStore = SubscriberAttributesStore(context),
                )
            }
        }
    }

    override val state: StateFlow<UiState>
        get() = _state.asStateFlow()

    private val _state = MutableStateFlow(UiState.Empty)

    init {
        updateAppUserID()
        updateApiKeyDescription()
        viewModelScope.launch {
            updateActiveEntitlements()
        }
    }

    override fun logIn(newAppUserId: String) {
        viewModelScope.launch {
            try {
                Purchases.sharedInstance.awaitLogIn(newAppUserId)
                updateAppUserID()
            } catch (e: PurchasesException) {
                _state.update { it.copy(appUserID = "Error logging in: ${e.message}") }
            }
        }
    }

    override fun logOut() {
        viewModelScope.launch {
            try {
                Purchases.sharedInstance.awaitLogOut()
                updateAppUserID()
            } catch (e: PurchasesException) {
                _state.update { it.copy(appUserID = "Error logging out: ${e.message}") }
            }
        }
    }

    override fun switchApiKey(newApiKey: String) {
        clearAllSubscriberAttributes()
        subscriberAttributesStore.clearAll()
        apiKeyStore.setLastUsedApiKey(newApiKey)
        configurePurchases(newApiKey)
        updateApiKeyDescription()
        updateAppUserID()
    }

    override fun refresh() {
        viewModelScope.launch {
            updateAppUserID()
            updateActiveEntitlements()
        }
    }

    override fun setSubscriberAttribute(key: String, value: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.isEmpty()) return
        Purchases.sharedInstance.setAttributes(mapOf(trimmedKey to value))
        val attributes = subscriberAttributesStore.set(Purchases.sharedInstance.appUserID, trimmedKey, value)
        _state.update { it.copy(subscriberAttributes = attributes) }
    }

    override fun clearSubscriberAttribute(key: String) {
        Purchases.sharedInstance.setAttributes(mapOf(key to null))
        val attributes = subscriberAttributesStore.remove(Purchases.sharedInstance.appUserID, key)
        _state.update { it.copy(subscriberAttributes = attributes) }
    }

    override fun clearAllSubscriberAttributes() {
        val keys = _state.value.subscriberAttributes.keys
        if (keys.isEmpty()) return
        Purchases.sharedInstance.setAttributes(keys.associateWith { null })
        val attributes = subscriberAttributesStore.clear(Purchases.sharedInstance.appUserID)
        _state.update { it.copy(subscriberAttributes = attributes) }
    }

    private fun updateAppUserID() {
        val appUserID = Purchases.sharedInstance.appUserID
        _state.update {
            it.copy(
                appUserID = appUserID,
                subscriberAttributes = subscriberAttributesStore.attributes(appUserID),
            )
        }
    }

    private fun updateApiKeyDescription() {
        _state.update {
            it.copy(
                apiKeyDescription = when (val apiKey = Purchases.sharedInstance.currentConfiguration.apiKey) {
                    Constants.GOOGLE_API_KEY_A -> Constants.GOOGLE_API_KEY_A_LABEL
                    Constants.GOOGLE_API_KEY_B -> Constants.GOOGLE_API_KEY_B_LABEL
                    else -> "Custom: $apiKey"
                },
            )
        }
    }

    private suspend fun updateActiveEntitlements() {
        val customerInfo = try {
            Purchases.sharedInstance.awaitCustomerInfo()
        } catch (e: PurchasesException) {
            _state.update { it.copy(activeEntitlements = listOf("Error fetching entitlements: ${e.message}")) }
            return
        }
        _state.update { it.copy(activeEntitlements = customerInfo.entitlements.active.keys.sorted()) }
    }
}
