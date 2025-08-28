package com.revenuecat.purchases.common

import androidx.core.os.LocaleListCompat
import java.util.Locale

internal interface LocaleProvider {
    val currentLocalesLanguageTags: String
}

internal class DefaultLocaleProvider : LocaleProvider {
    override val currentLocalesLanguageTags: String
        get() {
            // Check if there's a preferred locale override from Purchases instance
            val preferredLocale = getPreferredLocaleFromPurchases()
            return if (preferredLocale != null) {
                val defaultLocales = LocaleListCompat.getDefault().toLanguageTags()
                if (defaultLocales.isEmpty()) {
                    preferredLocale
                } else {
                    "$preferredLocale,$defaultLocales"
                }
            } else {
                LocaleListCompat.getDefault().toLanguageTags()
            }
        }
        
    private fun getPreferredLocaleFromPurchases(): String? {
        return try {
            val purchasesClass = Class.forName("com.revenuecat.purchases.Purchases")
            val sharedInstanceMethod = purchasesClass.getMethod("getSharedInstance")
            val purchasesInstance = sharedInstanceMethod.invoke(null)
            val getPreferredUILocaleOverrideMethod = purchasesClass.getMethod("getPreferredUILocaleOverride")
            val result = getPreferredUILocaleOverrideMethod.invoke(purchasesInstance) as String?
            android.util.Log.d("DefaultLocaleProvider", "Preferred locale from Purchases: $result")
            result
        } catch (e: Exception) {
            // If anything fails (Purchases not configured, reflection issues, etc.), return null
            android.util.Log.d("DefaultLocaleProvider", "Failed to get preferred locale: ${e.message}")
            null
        }
    }
}

internal class PurchasesAwareLocaleProvider(
    private val preferredLocaleOverrideProvider: () -> String?
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
