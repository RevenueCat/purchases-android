package com.revenuecat.sample.user

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.sample.utils.hasActiveEntitlements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<UserState> = MutableStateFlow(
        UserState(false, Purchases.sharedInstance.appUserID),
    )
    val uiState: StateFlow<UserState> = _uiState.asStateFlow()

    init {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                _uiState.update { it.copy(displayErrorMessage = error.message) }
            },
            onSuccess = { customerInfo ->
                _uiState.update {
                    it.copy(isSubscriber = customerInfo.entitlements.hasActiveEntitlements())
                }
            },
        )
    }

    fun initiateLogIn() {
        _uiState.update { it.copy(shouldStartLoginProcess = true) }
    }

    fun logIn(newUserId: String) {
        Purchases.sharedInstance.logInWith(
            appUserID = newUserId,
            onError = { error ->
                _uiState.update {
                    it.copy(
                        displayErrorMessage = error.message,
                        shouldStartLoginProcess = false,
                    )
                }
            },
            onSuccess = { customerInfo, _ ->
                _uiState.update {
                    it.copy(
                        currentUserId = Purchases.sharedInstance.appUserID,
                        isSubscriber = customerInfo.entitlements.hasActiveEntitlements(),
                        shouldStartLoginProcess = false,
                    )
                }
            },
        )
    }

    fun restorePurchases() {
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                _uiState.update { it.copy(displayErrorMessage = error.message) }
            },
            onSuccess = { customerInfo ->
                _uiState.update {
                    it.copy(isSubscriber = customerInfo.entitlements.hasActiveEntitlements())
                }
            },
        )
    }

    fun resetErrorMessage() {
        _uiState.update { it.copy(displayErrorMessage = null) }
    }

    fun resetLoginProcess() {
        _uiState.update { it.copy(shouldStartLoginProcess = false) }
    }
}
