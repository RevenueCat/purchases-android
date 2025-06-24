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

    fun isBackwardTransition(from: CustomerCenterDestination, to: CustomerCenterDestination): Boolean {
        // Simple rule: going to Main from any other screen is always backward
        if (to is CustomerCenterDestination.Main && from !is CustomerCenterDestination.Main) {
            return true
        }

        // For other cases, use the stack positions
        val fromIndex = destinationStack.indexOf(from)
        val toIndex = destinationStack.indexOf(to)

        // If either destination is not in the stack, assume forward transition
        return when {
            toIndex == -1 || fromIndex == -1 -> false
            else -> toIndex < fromIndex // backward means going to a lower index (closer to root)
        }
    }
}
