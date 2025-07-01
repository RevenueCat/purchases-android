package com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

internal object CustomerCenterAnimations {

    fun getTransitionForNavigation(
        from: CustomerCenterDestination,
        to: CustomerCenterDestination,
        navigationState: CustomerCenterNavigationState,
    ) = if (navigationState.isBackwardTransition(from, to)) {
        // Going backward - slide in from left
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
    } else {
        // Going forward - slide in from right
        slideInHorizontally(initialOffsetX = { it }) togetherWith
            slideOutHorizontally(targetOffsetX = { -it })
    }
}
