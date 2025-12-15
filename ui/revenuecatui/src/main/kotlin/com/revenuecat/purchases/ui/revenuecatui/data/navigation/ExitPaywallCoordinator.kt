package com.revenuecat.purchases.ui.revenuecatui.data.navigation

import java.util.ArrayDeque

internal class ExitPaywallCoordinator {

    private val stack = ArrayDeque<PaywallEntry>()
    private val shownExitOfferings = mutableSetOf<String>()
    private var purchaseCompleted = false

    fun reset() {
        stack.clear()
        shownExitOfferings.clear()
        purchaseCompleted = false
    }

    fun onPaywallPresented(offeringId: String, exitSettings: ExitOffersSettings?) {
        val entry = PaywallEntry(offeringId, exitSettings)
        val last = stack.lastOrNull()
        if (last?.offeringId == offeringId) {
            stack.removeLast()
        }
        stack.add(entry)
        purchaseCompleted = false
    }

    fun onPurchaseCompleted() {
        purchaseCompleted = true
    }

    fun onCloseRequested(): NavigationDecision {
        val current = stack.lastOrNull() ?: return NavigationDecision.Dismiss

        if (purchaseCompleted) {
            purchaseCompleted = false
            removeCurrent()
            return NavigationDecision.Dismiss
        }

        val exitOfferingId = current.exitSettings?.dismissOfferingId
        purchaseCompleted = false

        if (exitOfferingId.isNullOrBlank() || shownExitOfferings.contains(exitOfferingId)) {
            removeCurrent()
            return NavigationDecision.Dismiss
        }

        shownExitOfferings += exitOfferingId

        return NavigationDecision.ShowExitPaywall(offeringId = exitOfferingId)
    }

    fun hasActivePaywall(): Boolean = stack.isNotEmpty()

    fun isShowingExitPaywall(): Boolean = shownExitOfferings.isNotEmpty()

    private fun removeCurrent() {
        if (stack.isNotEmpty()) {
            stack.removeLast()
        }
        if (stack.isEmpty()) {
            shownExitOfferings.clear()
        }
    }

    private data class PaywallEntry(
        val offeringId: String,
        val exitSettings: ExitOffersSettings?,
    )

    sealed interface NavigationDecision {
        object Dismiss : NavigationDecision
        data class ShowExitPaywall(
            val offeringId: String,
        ) : NavigationDecision
    }
}
