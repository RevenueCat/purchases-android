package com.revenuecat.purchasetester.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchasetester.ui.model.login.PurchaseLoginState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface PurchaseLoginViewModel {
    val state: StateFlow<PurchaseLoginState>
    fun onAction(action: PurchaseLoginActions)
    fun onNavigationHandled()
}

class PurchaseLoginViewModelImpl : ViewModel(), PurchaseLoginViewModel {

    private val _state = MutableStateFlow(PurchaseLoginState())
    override val state: StateFlow<PurchaseLoginState> = _state.asStateFlow()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PurchaseLoginViewModelImpl()
            }
        }
    }

    override fun onAction(action: PurchaseLoginActions) {
        when (action) {
            is PurchaseLoginActions.OnLogin -> handleLogin(action.userId)
            PurchaseLoginActions.OnAnonymousUser -> handleAnonymousUser()
            PurchaseLoginActions.OnResetSdk -> handleResetSdk()
            PurchaseLoginActions.OnNavigateToLogs -> handleNavigateToLogs()
            PurchaseLoginActions.OnNavigateToProxy -> handleNavigateToProxy()
            PurchaseLoginActions.OnErrorDismissed -> dismissError()
        }
    }

    private fun handleLogin(userId: String) {
        if (userId.isBlank()) {
            showError("User ID cannot be empty")
            return
        }

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                Purchases.sharedInstance.logInWith(
                    userId,
                    { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    },
                    { _, _ ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                navigateToOverview = true
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun handleAnonymousUser() {
        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                if (Purchases.sharedInstance.isAnonymous) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            navigateToOverview = true
                        )
                    }
                } else {
                    Purchases.sharedInstance.logOutWith(
                        { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message
                                )
                            }
                        },
                        {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    navigateToOverview = true
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun handleResetSdk() {
        viewModelScope.launch {
            try {
                Purchases.sharedInstance.close()
                _state.update { it.copy(navigateToConfigure = true) }
            } catch (e: Exception) {
                showError("Failed to reset SDK: ${e.message}")
            }
        }
    }

    private fun handleNavigateToLogs() {
        _state.update { it.copy(navigateToLogs = true) }
    }

    private fun handleNavigateToProxy() {
        _state.update { it.copy(navigateToProxy = true) }
    }

    private fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    override fun onNavigationHandled() {
        _state.update {
            it.copy(
                navigateToOverview = false,
                navigateToConfigure = false,
                navigateToLogs = false,
                navigateToProxy = false
            )
        }
    }
}