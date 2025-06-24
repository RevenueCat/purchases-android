package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CustomerCenterNavigationViewModel : ViewModel() {

    private val _navigationState = MutableStateFlow(CustomerCenterNavigationState())
    val navigationState: StateFlow<CustomerCenterNavigationState> = _navigationState.asStateFlow()

    fun navigateTo(destination: CustomerCenterDestination) {
        _navigationState.value = _navigationState.value.push(destination)
    }

    fun navigateBack(): Boolean {
        return if (_navigationState.value.canNavigateBack) {
            _navigationState.value = _navigationState.value.pop()
            true
        } else {
            false
        }
    }

    fun navigateToMain() {
        _navigationState.value = _navigationState.value.popToMain()
    }

    fun replaceCurrentDestination(destination: CustomerCenterDestination) {
        _navigationState.value = _navigationState.value.replace(destination)
    }

    fun getCurrentDestination(): CustomerCenterDestination {
        return _navigationState.value.currentDestination
    }

    fun canNavigateBack(): Boolean {
        return _navigationState.value.canNavigateBack
    }

    fun getDestinationStack(): List<CustomerCenterDestination> {
        return _navigationState.value.backStack
    }
}
