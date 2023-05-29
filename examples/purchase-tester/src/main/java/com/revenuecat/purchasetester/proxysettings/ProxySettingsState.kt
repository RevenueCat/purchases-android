package com.revenuecat.purchasetester.proxysettings

sealed class ProxySettingsState {
    object Loading : ProxySettingsState()
    data class Error(val message: String) : ProxySettingsState()
    data class CurrentMode(val mode: ProxyMode) : ProxySettingsState()
}
