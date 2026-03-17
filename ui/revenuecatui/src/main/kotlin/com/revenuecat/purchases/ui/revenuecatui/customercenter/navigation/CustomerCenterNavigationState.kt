package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.ArrayDeque
import java.util.Deque

@Immutable
internal data class CustomerCenterNavigationState(
    private val showingActivePurchasesScreen: Boolean,
    private val managementScreenTitle: String?,
    val backStack: Deque<CustomerCenterDestination> = ArrayDeque<CustomerCenterDestination>().apply {
        push(CustomerCenterDestination.Main(showingActivePurchasesScreen, managementScreenTitle))
    },
) {
    val currentDestination: CustomerCenterDestination
        get() = backStack.peek() ?: CustomerCenterDestination.Main(showingActivePurchasesScreen, managementScreenTitle)

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

    /**
     * Reconciles the back stack with refreshed purchases.
     * For each [CustomerCenterDestination.SelectedPurchaseDetail] in the stack, finds the matching
     * purchase in [refreshedPurchases] by product ID. If found, replaces the destination's
     * purchaseInformation with the refreshed one. If not found, pops to main since the purchase
     * no longer exists.
     *
     * @return the reconciled navigation state, or null if any purchase in the stack was removed
     * and the state should pop to main.
     */
    fun reconcileWithPurchases(
        refreshedPurchases: List<com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation>,
    ): CustomerCenterNavigationState? {
        val newStack = ArrayDeque<CustomerCenterDestination>()
        // backStack is a Deque where peek() returns the top (most recent) element.
        // We iterate from bottom to top to preserve order when pushing onto the new stack.
        val stackList = backStack.toList().reversed()
        for (destination in stackList) {
            when (destination) {
                is CustomerCenterDestination.SelectedPurchaseDetail -> {
                    val productId = destination.purchaseInformation.product?.id
                    val refreshedPurchase = refreshedPurchases.firstOrNull { it.product?.id == productId }
                    if (refreshedPurchase != null) {
                        newStack.push(destination.copy(purchaseInformation = refreshedPurchase))
                    } else {
                        // Purchase no longer exists — signal to pop to main
                        return null
                    }
                }
                else -> newStack.push(destination)
            }
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
