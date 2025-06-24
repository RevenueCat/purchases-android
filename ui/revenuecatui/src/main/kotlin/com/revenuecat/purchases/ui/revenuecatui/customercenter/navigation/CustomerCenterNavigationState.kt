package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import java.util.ArrayDeque
import java.util.Deque

@Immutable
internal data class CustomerCenterNavigationState(
    val backStack: Deque<CustomerCenterDestination> = ArrayDeque<CustomerCenterDestination>().apply {
        push(CustomerCenterDestination.Main)
    },
) {
    val currentDestination: CustomerCenterDestination
        get() = backStack.peek() ?: CustomerCenterDestination.Main

    val canNavigateBack: Boolean
        get() = backStack.size > 1

    fun push(destination: CustomerCenterDestination): CustomerCenterNavigationState {
        val newStack = ArrayDeque(backStack)
        newStack.push(destination)
        return copy(backStack = newStack)
    }

    fun pop(): CustomerCenterNavigationState {
        return if (canNavigateBack) {
            val newStack = ArrayDeque(backStack)
            newStack.pop()
            copy(backStack = newStack)
        } else {
            this
        }
    }

    fun popToMain(): CustomerCenterNavigationState {
        val newStack = ArrayDeque<CustomerCenterDestination>()
        newStack.push(CustomerCenterDestination.Main)
        return copy(backStack = newStack)
    }

    fun replace(destination: CustomerCenterDestination): CustomerCenterNavigationState {
        val newStack = ArrayDeque(backStack)
        if (newStack.isNotEmpty()) {
            newStack.pop()
        }
        newStack.push(destination)
        return copy(backStack = newStack)
    }

    fun isBackwardTransition(from: CustomerCenterDestination, to: CustomerCenterDestination): Boolean {
        // Simple rule: going to Main from any other screen is always backward
        if (to is CustomerCenterDestination.Main && from !is CustomerCenterDestination.Main) {
            return true
        }

        // For other cases, use the stack positions
        val stackList = backStack.toList()
        // 0 will be the top of the backStack
        val fromIndex = stackList.indexOf(from)
        val toIndex = stackList.indexOf(to)

        // If either destination is not in the stack, assume forward transition
        return when {
            toIndex == -1 || fromIndex == -1 -> false
            else -> toIndex > fromIndex // backward means going to a higher index
        }
    }
}
