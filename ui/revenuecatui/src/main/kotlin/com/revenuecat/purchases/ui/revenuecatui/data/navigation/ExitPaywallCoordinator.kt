package com.revenuecat.purchases.ui.revenuecatui.data.navigation

import com.revenuecat.purchases.paywalls.components.common.ExitPaywallPresentation
import java.util.ArrayDeque

internal class ExitPaywallCoordinator {

    private val stack = ArrayDeque<PaywallEntry>()
    private val shownExitOfferings = mutableSetOf<String>()
    private var purchaseInteraction: PurchaseInteraction = PurchaseInteraction.NONE

    fun reset() {
        stack.clear()
        shownExitOfferings.clear()
        purchaseInteraction = PurchaseInteraction.NONE
    }

    fun onPaywallPresented(offeringId: String, exitSettings: ExitPaywallSettings?) {
        val entry = PaywallEntry(offeringId, exitSettings)
        val last = stack.lastOrNull()
        if (last?.offeringId == offeringId) {
            stack.removeLast()
        }
        stack.add(entry)
        purchaseInteraction = PurchaseInteraction.NONE
    }

    fun onPurchaseStarted() {
        purchaseInteraction = PurchaseInteraction.STARTED
    }

    fun onPurchaseCancelled() {
        purchaseInteraction = PurchaseInteraction.CANCELLED
    }

    fun onPurchaseCompleted() {
        purchaseInteraction = PurchaseInteraction.COMPLETED
    }

    fun resetPurchaseInteraction() {
        purchaseInteraction = PurchaseInteraction.NONE
    }

    fun onCloseRequested(): NavigationDecision {
        val current = stack.lastOrNull() ?: return NavigationDecision.Dismiss
        val nextExit = when (purchaseInteraction) {
            PurchaseInteraction.CANCELLED,
            PurchaseInteraction.STARTED,
            -> current.exitSettings?.abandonment
            PurchaseInteraction.COMPLETED -> null
            PurchaseInteraction.NONE -> current.exitSettings?.bounce
        }

        purchaseInteraction = PurchaseInteraction.NONE

        if (nextExit == null || nextExit.offeringId.isBlank() || shownExitOfferings.contains(nextExit.offeringId)) {
            removeCurrent()
            return NavigationDecision.Dismiss
        }

        shownExitOfferings += nextExit.offeringId
        if (nextExit.dismissCurrent) {
            removeCurrent()
        }

        return NavigationDecision.ShowExitPaywall(
            offeringId = nextExit.offeringId,
            presentation = nextExit.presentation,
            replaceCurrent = nextExit.dismissCurrent,
        )
    }

    fun hasActivePaywall(): Boolean = stack.isNotEmpty()

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
        val exitSettings: ExitPaywallSettings?,
    )

    private enum class PurchaseInteraction {
        NONE,
        STARTED,
        CANCELLED,
        COMPLETED,
    }

    sealed interface NavigationDecision {
        object Dismiss : NavigationDecision
        data class ShowExitPaywall(
            val offeringId: String,
            val presentation: ExitPaywallPresentation,
            val replaceCurrent: Boolean,
        ) : NavigationDecision
    }
}
