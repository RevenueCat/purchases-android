package com.revenuecat.purchases.ui.revenuecatui.utils

fun interface Invokable {
    fun performAction()

    operator fun invoke() = performAction()
}

