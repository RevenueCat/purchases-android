package com.revenuecat.sample.main

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainViewModel : ViewModel() {
    private val previewMode = true

    private val _uiState: MutableStateFlow<MainState> = MutableStateFlow(
        MainState(),
    )
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    init {
        if (!previewMode) {
            Purchases.sharedInstance.updatedCustomerInfoListener =
                UpdatedCustomerInfoListener { customerInfo ->
                    updateCustomerInfoInformation(customerInfo)
                }
            viewModelScope.launch {
                getOfferings()

            }
        }
    }

    private suspend fun getOfferings() {
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            _uiState.update { it.copy(offerings = offerings) }
        } catch (error: PurchasesException) {
            _uiState.update { it.copy(displayErrorMessage = error.message) }
        }
    }

    fun switchUser(newUserID: String) {
        Purchases.sharedInstance.switchUser(newUserID)
        _uiState.update { it.copy(currentAppUserID = newUserID) }
    }

    fun resetErrorMessage() {
        _uiState.update { it.copy(displayErrorMessage = null) }
    }

    fun initiateSwitchUserProcess() {
        _uiState.update { it.copy(shouldShowSwitchingUserDialog = true) }
    }

    fun resetSwitchUserProcess() {
        _uiState.update { it.copy(shouldShowSwitchingUserDialog = false) }
    }

    fun purchasePackage(activity: Activity, aPackage: Package) {
        viewModelScope.launch {
            val purchaseParams = PurchaseParams.Builder(activity, aPackage).build()
            try {
                val (transaction, customerInfo) =
                    Purchases.sharedInstance.awaitPurchase(purchaseParams)
                val logMessage = "Purchase finished:\nTransaction: $transaction\n" +
                    "CustomerInfo: $customerInfo"
                Log.d("Purchase", logMessage)
            } catch (error: PurchasesTransactionException) {
                if (error.userCancelled) {
                    _uiState.update { it.copy(displayErrorMessage = "User cancelled") }
                } else {
                    _uiState.update { it.copy(displayErrorMessage = error.message) }
                }
            }

        }
    }

    private fun updateCustomerInfoInformation(customerInfo: CustomerInfo) {
        _uiState.update {
            it.copy(
                currentCustomerInfo = customerInfo,
                currentAppUserID = Purchases.sharedInstance.appUserID
            )
        }
    }

    fun dismissExplanationDialog() {
        _uiState.update { it.copy(shouldShowExplanationDialog = false) }
    }

    fun showExplanationDialog() {
        _uiState.update { it.copy(shouldShowExplanationDialog = true) }
    }

}
