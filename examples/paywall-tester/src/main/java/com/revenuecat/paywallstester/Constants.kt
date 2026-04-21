package com.revenuecat.paywallstester

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

@Suppress("MagicNumber")
object Constants {
    // PaywallsTester supports switching between 2 API keys on the App Info screen. It uses "A" by default.
    // Set PAYWALL_TESTER_API_KEY_A / B in local.properties to configure these at build time.
    val GOOGLE_API_KEY_A: String = BuildConfig.PAYWALL_TESTER_API_KEY_A.ifEmpty { "API_KEY_A" }
    val GOOGLE_API_KEY_B: String = BuildConfig.PAYWALL_TESTER_API_KEY_B.ifEmpty { "API_KEY_B" }

    // PaywallsTester will use these labels in its UI to differentiate the API keys.
    val GOOGLE_API_KEY_A_LABEL: String = BuildConfig.PAYWALL_TESTER_API_KEY_A_LABEL.ifEmpty { "API_KEY_A_LABEL" }
    val GOOGLE_API_KEY_B_LABEL: String = BuildConfig.PAYWALL_TESTER_API_KEY_B_LABEL.ifEmpty { "API_KEY_B_LABEL" }

    // Optional: Set a preferred UI locale override (e.g., "en-US", "es-ES", "fr-FR")
    // Leave as empty string to use system default locale
    const val PREFERRED_UI_LOCALE_OVERRIDE: String = ""

    // Custom variables to pass to paywalls. These will replace {{ custom.key }} placeholders.
    val CUSTOM_VARIABLES: Map<String, CustomVariableValue> = mapOf(
        "user_name" to CustomVariableValue.String("John"),
        "app_name" to CustomVariableValue.String("Paywall Tester"),
        "user_points" to CustomVariableValue.Number(100),
    )
}
