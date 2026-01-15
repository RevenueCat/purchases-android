package com.revenuecat.purchases.ui.revenuecatui.utils

fun interface Resumable {
    fun resume(shouldResume: Boolean)

    operator fun invoke(shouldResume: Boolean = true) = resume(shouldResume)
}
