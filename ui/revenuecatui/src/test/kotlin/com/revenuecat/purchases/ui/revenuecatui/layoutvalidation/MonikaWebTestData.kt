package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal object MonikaWebTestData : PaywallLayoutValidationFixture {

    override val offeringId: String get() = OFFERING_ID

    override val offering: Offering by lazy {
        val paywallComponentsJson = paywallComponentsJson()

        Offering(
            identifier = OFFERING_ID,
            serverDescription = DASHBOARD_PAYWALL_NAME,
            metadata = emptyMap(),
            availablePackages = packages,
            paywall = null,
            paywallComponents = Offering.PaywallComponents(
                uiConfig = uiConfig,
                data = json.decodeFromString(paywallComponentsJson.toString()),
            ),
        )
    }

    override val componentsConfigJson: JSONObject by lazy {
        JSONObject(componentsFile.readText())
    }

    private val packages: List<Package>
        get() {
            val presentedOfferingContext = PresentedOfferingContext(OFFERING_ID)
            return listOf(
                TestData.Packages.monthly.copyFor(presentedOfferingContext),
                TestData.Packages.annual.copyFor(presentedOfferingContext),
                TestData.Packages.quarterly.copyFor(presentedOfferingContext),
                TestData.Packages.semester.copyFor(presentedOfferingContext),
                TestData.Packages.weekly.copyFor(presentedOfferingContext),
                TestData.Packages.lifetime.copyFor(presentedOfferingContext),
                Package(
                    identifier = "Lifetime Lite",
                    packageType = PackageType.CUSTOM,
                    product = TestData.Packages.lifetime.product,
                    presentedOfferingContext = presentedOfferingContext,
                ),
            )
        }

    private fun Package.copyFor(presentedOfferingContext: PresentedOfferingContext): Package {
        return Package(
            identifier = identifier,
            packageType = packageType,
            product = product,
            presentedOfferingContext = presentedOfferingContext,
            webCheckoutURL = webCheckoutURL,
        )
    }

    private val componentsFile: File
        get() = File(
            BuildConfig.PROJECT_DIR,
            "src/test/resources/layoutvalidation/paywall-pw5f617adf639240af-components.json",
        )

    private val localizationsFile: File
        get() = File(
            BuildConfig.PROJECT_DIR,
            "src/test/resources/layoutvalidation/paywall-pw5f617adf639240af-localizations.json",
        )

    private val uiConfig: UiConfig
        get() = UiConfig(
            localizations = mapOf(
                LocaleId(DEFAULT_LOCALE) to variableLocalizationKeysForEnUs(),
            ),
        )

    private fun paywallComponentsJson(): JSONObject {
        require(componentsFile.exists()) {
            "Missing monika-web components fixture at ${componentsFile.absolutePath}."
        }
        require(localizationsFile.exists()) {
            "Missing monika-web localizations fixture at ${localizationsFile.absolutePath}."
        }

        val componentsConfig = JSONObject(componentsFile.readText())
        val localizations = JSONObject(localizationsFile.readText())
        val defaultLocale = localizations.optString("default_locale", DEFAULT_LOCALE)
        val componentsLocalizations = localizations.optJSONObject("components_localizations")
            ?: localizations.optJSONObject("localized_strings")
            ?: JSONObject().put(defaultLocale, generatedFallbackLocalizations(componentsConfig))

        return JSONObject()
            .put("id", DASHBOARD_PAYWALL_ID)
            .put("template_name", TEMPLATE_NAME)
            .put("asset_base_url", ASSET_BASE_URL)
            .put("components_config", componentsConfig)
            .put("components_localizations", componentsLocalizations)
            .put("default_locale", defaultLocale)
    }

    private fun generatedFallbackLocalizations(jsonObject: JSONObject): JSONObject {
        val localizations = JSONObject()
        collectLocalizationKeys(jsonObject, localizations)
        return localizations
    }

    private fun collectLocalizationKeys(value: Any?, localizations: JSONObject) {
        when (value) {
            is JSONObject -> {
                value.optString("text_lid").takeIf { it.isNotBlank() }?.let { localizations.put(it, it) }
                value.optString("url_lid").takeIf { it.isNotBlank() }?.let { localizations.put(it, it) }
                value.keys().forEach { key -> collectLocalizationKeys(value.opt(key), localizations) }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectLocalizationKeys(value.opt(index), localizations)
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private const val OFFERING_ID = "monika-web"
    private const val DASHBOARD_PAYWALL_ID = "pw5f617adf639240af"
    private const val DASHBOARD_PAYWALL_NAME = "Monika Web Paywall"
    private const val TEMPLATE_NAME = "components"
    private const val ASSET_BASE_URL = "https://assets.pawwalls.com"
    private const val DEFAULT_LOCALE = "en_US"
}
