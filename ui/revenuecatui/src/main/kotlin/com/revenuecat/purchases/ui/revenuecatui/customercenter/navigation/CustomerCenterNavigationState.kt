package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable

@Immutable
internal data class CustomerCenterNavigationState(
    val destinationStack: List<CustomerCenterDestination> = listOf(CustomerCenterDestination.Main),
) {
    val currentDestination: CustomerCenterDestination
        get() = destinationStack.last()

    val canNavigateBack: Boolean
        get() = destinationStack.size > 1

    fun push(destination: CustomerCenterDestination): CustomerCenterNavigationState {
        return copy(destinationStack = destinationStack + destination)
    }

    fun pop(): CustomerCenterNavigationState {
        return if (canNavigateBack) {
            copy(destinationStack = destinationStack.dropLast(1))
        } else {
            this
        }
    }

    fun popToMain(): CustomerCenterNavigationState {
        return copy(destinationStack = listOf(CustomerCenterDestination.Main))
    }

    fun replace(destination: CustomerCenterDestination): CustomerCenterNavigationState {
        return copy(destinationStack = destinationStack.dropLast(1) + destination)
    }
}
