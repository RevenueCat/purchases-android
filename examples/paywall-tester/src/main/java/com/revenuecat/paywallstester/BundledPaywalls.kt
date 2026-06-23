@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.paywallstester

import android.content.Context
import android.util.Log
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.URL

/**
 * Loads paywalls that are bundled with the app as JSON assets (see `assets/bundled_paywalls`), instead
 * of being fetched from the RevenueCat server. These are used to exercise Paywalls V2 features – such as
 * the `web_view` component – without needing a matching offering configured in the dashboard.
 *
 * Each asset only contains the [ComponentsConfig] (the `{ "base": { ... } }` structure). The surrounding
 * [PaywallComponentsData] (template name, asset base URL, localizations, …) is supplied here in code.
 */
object BundledPaywalls {

    private const val TAG = "BundledPaywalls"
    private const val ASSET_DIR = "bundled_paywalls"
    private val assetBaseURL = URL("https://assets.pawwalls.com")
    private val defaultLocale = LocaleId("en_US")

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private data class BundledPaywall(
        val offeringId: String,
        val assetFileName: String,
        val templateName: String,
    )

    private val paywallDefinitions = listOf(
        BundledPaywall(
            offeringId = "bundled_webview_cat",
            assetFileName = "components-cat.json",
            templateName = "webview",
        ),
        BundledPaywall(
            offeringId = "bundled_webview_dog",
            assetFileName = "components-dog.json",
            templateName = "webview",
        ),
    )

    // Localizations for the `text_lid`/`url_lid` keys referenced by the bundled component JSONs.
    private val localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>> = mapOf(
        defaultLocale to mapOf(
            // Monthly package
            LocalizationKey("b11T5KqURa") to LocalizationData.Text("Monthly"),
            LocalizationKey("3PUpQs2Ysa") to LocalizationData.Text("$6.99 / month"),
            // Annual package
            LocalizationKey("pAwLNYbZ2e") to LocalizationData.Text("Best value"),
            LocalizationKey("Hjy1nCtkxJ") to LocalizationData.Text("Annual"),
            LocalizationKey("tzMoZfGK7q") to LocalizationData.Text("$53.99 / year"),
            // Purchase button
            LocalizationKey("uZcnB1JkxA") to LocalizationData.Text("Continue"),
            // Footer buttons
            LocalizationKey("n1dzcYoLL1") to LocalizationData.Text("Restore purchases"),
            LocalizationKey("9pHXVuJRXm") to LocalizationData.Text("Terms"),
            LocalizationKey("PXTOmY0Zc3") to LocalizationData.Text("Privacy"),
            // URLs referenced by `navigate_to` actions
            LocalizationKey("92rZFECc4Z") to LocalizationData.Text("https://www.revenuecat.com/terms"),
            LocalizationKey("VBPJOj-Wkx") to LocalizationData.Text("https://www.revenuecat.com/privacy"),
        ),
    )

    @Volatile
    private var cachedOfferings: List<Offering>? = null

    /**
     * Returns the bundled offerings, parsed from the JSON assets. The result is cached after the first
     * successful load. Paywalls that fail to parse are skipped.
     */
    fun offerings(context: Context): List<Offering> {
        cachedOfferings?.let { return it }
        return synchronized(this) {
            cachedOfferings ?: paywallDefinitions
                .mapNotNull { loadOffering(context, it) }
                .also { cachedOfferings = it }
        }
    }

    /**
     * Returns the bundled offering with the given [offeringId], or `null` if there is none. Used so that
     * navigation by offering identifier resolves bundled paywalls too.
     */
    fun offeringById(context: Context, offeringId: String): Offering? =
        offerings(context).firstOrNull { it.identifier == offeringId }

    private fun loadOffering(context: Context, paywall: BundledPaywall): Offering? =
        runCatching {
            val componentsJson = context.assets
                .open("$ASSET_DIR/${paywall.assetFileName}")
                .bufferedReader()
                .use { it.readText() }
            val componentsConfig = json.decodeFromString(ComponentsConfig.serializer(), componentsJson)

            Offering(
                identifier = paywall.offeringId,
                serverDescription = "Bundled paywall (${paywall.assetFileName})",
                metadata = emptyMap(),
                availablePackages = SamplePaywalls.packages,
                paywallComponents = Offering.PaywallComponents(
                    uiConfig = UiConfig(
                        app = UiConfig.AppConfig(),
                        localizations = mapOf(defaultLocale to variableLocalizationKeysForEnUs()),
                    ),
                    data = PaywallComponentsData(
                        id = paywall.offeringId,
                        templateName = paywall.templateName,
                        assetBaseURL = assetBaseURL,
                        componentsConfig = componentsConfig,
                        componentsLocalizations = localizations,
                        defaultLocaleIdentifier = defaultLocale,
                    ),
                ),
            )
        }.onFailure {
            Log.e(TAG, "Failed to load bundled paywall '${paywall.assetFileName}'", it)
        }.getOrNull()
}
