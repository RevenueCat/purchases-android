package com.revenuecat.purchases.common

import androidx.core.os.LocaleListCompat

internal interface LocaleProvider {
    val currentLocalesLanguageTags: String
}

internal class DefaultLocaleProvider : LocaleProvider {
    private var orchestratorProvider: (() -> com.revenuecat.purchases.PurchasesOrchestrator?)? = null

    fun setOrchestratorProvider(provider: () -> com.revenuecat.purchases.PurchasesOrchestrator?) {
        orchestratorProvider = provider
    }

    override val currentLocalesLanguageTags: String
        get() {
            val preferredOverride = orchestratorProvider?.invoke()?.preferredUILocaleOverride
            val result = if (preferredOverride != null) {
                val defaultLocales = LocaleListCompat.getDefault().toLanguageTags()
                if (defaultLocales.isEmpty()) {
                    preferredOverride
                } else {
                    "$preferredOverride,$defaultLocales"
                }
            } else {
                LocaleListCompat.getDefault().toLanguageTags()
            }
            android.util.Log.d(
                "DefaultLocale",
                "currentLocalesLanguageTags: preferredOverride='$preferredOverride' -> result='$result'",
            )
            return result
        }
}
