package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.ArrayDeque
import java.util.Deque

@Immutable
internal data class CustomerCenterNavigationState(
    private val mainTitle: String?,
    val backStack: Deque<CustomerCenterDestination> = ArrayDeque<CustomerCenterDestination>().apply {
        // Setting this to null allows the Main destination to be displayed without a title until we have a top bar
        push(CustomerCenterDestination.Main(title = null))
    },
) {
    val currentDestination: CustomerCenterDestination
        get() = backStack.peek() ?: CustomerCenterDestination.Main(title = null)

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
        val newStack = ArrayDeque(backStack)

        while (newStack.isNotEmpty() && newStack.peek() !is CustomerCenterDestination.Main) {
            newStack.pop()
        }

        if (newStack.isEmpty()) {
            Logger.e("Could not find Main destination in the back stack. Returning unchanged state.")
            return this
        }

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

        return when {
            toIndex == -1 || fromIndex == -1 -> {
                Logger.e(
                    "One of the destinations ($from [$fromIndex], $to [$toIndex]) is not in the back stack. " +
                        "Assuming forward transition.",
                )
                false
            }
            else -> toIndex > fromIndex // backward means going to a higher index
        }
    }
}
