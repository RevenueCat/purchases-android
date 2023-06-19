package com.revenuecat.purchases.debugview.models

internal sealed class SettingState(open val title: String) {
    data class Loading(override val title: String) : SettingState(title)
    data class TextLoaded(override val title: String, val content: String) : SettingState(title)
}
