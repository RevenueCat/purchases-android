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
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<MainState> = MutableStateFlow(
        MainState(),
    )
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    init {
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { customerInfo ->
                updateCustomerInfoInformation(customerInfo)
            }
        viewModelScope.launch {
            getOfferings()
        }
    }

    private suspend fun getOfferings() {
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            _uiState.update { currentState ->
                currentState.copy(
                    offerings = offerings,
                )
            }
        } catch (error: PurchasesException) {
            _uiState.update { currentState ->
                currentState.copy(
                    displayErrorMessage = error.message,
                )
            }
        }
    }

    fun switchUser(newUserID: String) {
        Purchases.sharedInstance.switchUser(newUserID)
    }

    fun resetErrorMessage() {
        _uiState.update { currentState ->
            currentState.copy(
                displayErrorMessage = null,
            )
        }
    }

    fun initiateSwitchUserProcess() {
        _uiState.update { it.copy(shouldStartSwitchingUser = true) }
    }

    fun resetSwitchUserProcess() {
        _uiState.update { it.copy(shouldStartSwitchingUser = false) }
    }

    fun purchasePackage(activity: Activity, aPackage: Package) {
        // TODO: use coroutine
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, aPackage).build(),
            onError = { error, userCancelled ->
                if (userCancelled) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            displayErrorMessage = "User cancelled",
                        )
                    }
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            displayErrorMessage = error.message,
                        )
                    }
                }
            },
            onSuccess = { transaction, customerInfo ->
                """
                Purchase finished:
                Transaction: $transaction
                CustomerInfo: $customerInfo
                """.trimIndent().also {
                    Log.d("Purchase", it)
                }
            },
        )
    }

    private fun updateCustomerInfoInformation(customerInfo: CustomerInfo) {
        _uiState.update { currentState ->
            currentState.copy(
                currentCustomerInfo = customerInfo,
            )
        }
    }
}
