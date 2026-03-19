@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class NavHostState(startPageId: String) {
    var currentPageId by mutableStateOf(startPageId)
        private set

    private val backStack = mutableStateListOf<String>()

    var direction by mutableStateOf(Direction.FORWARD)
        private set

    val canNavigateBack: Boolean
        get() = backStack.isNotEmpty()

    fun navigateTo(pageId: String) {
        direction = Direction.FORWARD
        backStack.add(currentPageId)
        currentPageId = pageId
    }

    fun navigateBack(): Boolean {
        if (!canNavigateBack) return false
        direction = Direction.BACKWARD
        currentPageId = backStack.removeLast()
        return true
    }

    enum class Direction { FORWARD, BACKWARD }
}
