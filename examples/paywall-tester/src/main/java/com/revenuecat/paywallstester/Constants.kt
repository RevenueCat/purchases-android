package com.revenuecat.paywallstester

object Constants {
    // PaywallsTester supports switching between 2 API keys on the App Info screen. It uses "A" by default.
    const val GOOGLE_API_KEY_A = "API_KEY_A"
    const val GOOGLE_API_KEY_B = "API_KEY_B"

    // PaywallsTester will use these labels in its UI to differentiate the API keys.
    const val GOOGLE_API_KEY_A_LABEL = "API_KEY_A_LABEL"
    const val GOOGLE_API_KEY_B_LABEL = "API_KEY_B_LABEL"

    // Optional: Set a preferred UI locale override (e.g., "en-US", "es-ES", "fr-FR")
    // Leave as empty string to use system default locale
    const val PREFERRED_UI_LOCALE_OVERRIDE: String = ""
}
