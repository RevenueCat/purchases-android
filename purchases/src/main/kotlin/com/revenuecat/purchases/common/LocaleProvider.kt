package com.revenuecat.purchases.common

import androidx.core.os.LocaleListCompat

internal interface LocaleProvider {
    val currentLocalesLanguageTags: String
}

internal class DefaultLocaleProvider : LocaleProvider {
    override val currentLocalesLanguageTags: String
        get() = LocaleListCompat.getDefault().toLanguageTags()
}

internal class PurchasesAwareLocaleProvider(
    private val preferredLocaleOverrideProvider: () -> String?,
) : LocaleProvider {
    override val currentLocalesLanguageTags: String
        get() {
            val preferredOverride = preferredLocaleOverrideProvider()
            return if (preferredOverride != null) {
                // If there's a preferred override, put it first in the list
                val defaultLocales = LocaleListCompat.getDefault().toLanguageTags()
                if (defaultLocales.isEmpty()) {
                    preferredOverride
                } else {
                    "$preferredOverride,$defaultLocales"
                }
            } else {
                LocaleListCompat.getDefault().toLanguageTags()
            }
        }
}

internal class OrchestrationAwareLocaleProvider : LocaleProvider {
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
                "OrchestrationAwareLocaleProvider",
                "currentLocalesLanguageTags: preferredOverride='$preferredOverride' -> result='$result'",
            )
            return result
        }
}
