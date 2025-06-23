package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CustomerCenterNavigationViewModel : ViewModel() {

    private val _navigationState = MutableStateFlow(CustomerCenterNavigationState())
    val navigationState: StateFlow<CustomerCenterNavigationState> = _navigationState.asStateFlow()

    private var lastStackSize = 1 // Start with Main screen
    private var isLastActionBackward = false

    fun navigateTo(destination: CustomerCenterDestination) {
        val newState = _navigationState.value.push(destination)
        updateStackTracking(newState)
        _navigationState.value = newState
    }

    fun navigateBack(): Boolean {
        return if (_navigationState.value.canNavigateBack) {
            val newState = _navigationState.value.pop()
            updateStackTracking(newState)
            _navigationState.value = newState
            true
        } else {
            false
        }
    }

    fun navigateToMain() {
        val newState = _navigationState.value.popToMain()
        updateStackTracking(newState)
        _navigationState.value = newState
    }

    fun replaceCurrentDestination(destination: CustomerCenterDestination) {
        val newState = _navigationState.value.replace(destination)
        updateStackTracking(newState)
        _navigationState.value = newState
    }

    fun getCurrentDestination(): CustomerCenterDestination {
        return _navigationState.value.currentDestination
    }

    fun canNavigateBack(): Boolean {
        return _navigationState.value.canNavigateBack
    }

    fun getDestinationStack(): List<CustomerCenterDestination> {
        return _navigationState.value.destinationStack
    }

    fun isLastActionBackward(): Boolean {
        return isLastActionBackward
    }

    private fun updateStackTracking(newState: CustomerCenterNavigationState) {
        val newStackSize = newState.destinationStack.size
        isLastActionBackward = newStackSize < lastStackSize
        lastStackSize = newStackSize
    }
}
