package com.revenuecat.paywallstester.ui.screens.main.appinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.paywallstester.ConfigurePurchasesUseCase
import com.revenuecat.paywallstester.Constants
import com.revenuecat.paywallstester.data.ApiKeyStore
import com.revenuecat.paywallstester.ui.screens.main.appinfo.AppInfoScreenViewModel.UiState
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
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
    ) {
        companion object {
            val Empty = UiState(
                appUserID = "",
                apiKeyDescription = "",
            )
        }
    }

    val state: StateFlow<UiState>

    fun logIn(newAppUserId: String)
    fun logOut()
    fun switchApiKey(newApiKey: String)
}

internal class AppInfoScreenViewModelImpl(
    private val configurePurchases: ConfigurePurchasesUseCase,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel(), AppInfoScreenViewModel {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                AppInfoScreenViewModelImpl(
                    configurePurchases = ConfigurePurchasesUseCase(context),
                    apiKeyStore = ApiKeyStore(context),
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
        apiKeyStore.setLastUsedApiKey(newApiKey)
        configurePurchases(newApiKey)
        updateApiKeyDescription()
    }

    private fun updateAppUserID() {
        _state.update { it.copy(appUserID = Purchases.sharedInstance.appUserID) }
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
}
