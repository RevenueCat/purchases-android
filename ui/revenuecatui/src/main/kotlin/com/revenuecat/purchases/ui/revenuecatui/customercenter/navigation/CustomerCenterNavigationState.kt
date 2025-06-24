package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable

@Immutable
internal data class CustomerCenterNavigationState(
    val backStack: List<CustomerCenterDestination> = listOf(CustomerCenterDestination.Main),
) {
    val currentDestination: CustomerCenterDestination
        get() = backStack.last()

    val canNavigateBack: Boolean
        get() = backStack.size > 1

    fun push(destination: CustomerCenterDestination): CustomerCenterNavigationState {
        return copy(backStack = backStack + destination)
    }

    fun pop(): CustomerCenterNavigationState {
        return if (canNavigateBack) {
            copy(backStack = backStack.dropLast(1))
        } else {
            this
        }
    }

    fun popToMain(): CustomerCenterNavigationState {
        return copy(backStack = listOf(CustomerCenterDestination.Main))
    }

    fun replace(destination: CustomerCenterDestination): CustomerCenterNavigationState {
        return copy(backStack = backStack.dropLast(1) + destination)
    }

    fun isBackwardTransition(from: CustomerCenterDestination, to: CustomerCenterDestination): Boolean {
        // Simple rule: going to Main from any other screen is always backward
        if (to is CustomerCenterDestination.Main && from !is CustomerCenterDestination.Main) {
            return true
        }

        // For other cases, use the stack positions
        val fromIndex = backStack.indexOf(from)
        val toIndex = backStack.indexOf(to)

        // If either destination is not in the stack, assume forward transition
        return when {
            toIndex == -1 || fromIndex == -1 -> false
            else -> toIndex < fromIndex // backward means going to a lower index (closer to root)
        }
    }
}
