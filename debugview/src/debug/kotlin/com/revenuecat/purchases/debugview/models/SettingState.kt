package com.revenuecat.purchases.debugview.models

internal sealed class SettingState(open val title: String) {
    data class Text(override val title: String, val content: String) : SettingState(title)
}
