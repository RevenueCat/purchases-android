package com.revenuecat.purchases.common

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools.json
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.utils.getNullableString
import com.revenuecat.purchases.utils.optNullableInt
import com.revenuecat.purchases.utils.optNullableString
import com.revenuecat.purchases.utils.replaceJsonNullWithKotlinNull
import com.revenuecat.purchases.utils.toMap
import com.revenuecat.purchases.withPresentedContext
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL

internal abstract class OfferingParser(
    // Whether to actually build [Offering.PaywallComponents] (capturing the raw component JSON) when an offering
    // carries `paywall_components`. Evaluated per parse: under workflows with remote config still enabled the
    // components are served from `/v1/config`, so capturing them here is dead memory. Reverts to `true` once the
    // 4xx kill switch disables remote config, so a subsequent refetch decodes them for the fallback render path.
    private val shouldParsePaywallComponents: () -> Boolean = { true },
) {

    protected abstract fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct?

    /**
     * Note: this may return an empty Offerings.
     */
    @OptIn(InternalRevenueCatAPI::class)
    @JvmOverloads
    fun createOfferings(
        offeringsJson: JSONObject,
        productsById: Map<String, List<StoreProduct>>,
        originalSource: HTTPResponseOriginalSource = HTTPResponseOriginalSource.MAIN,
        loadedFromDiskCache: Boolean = false,
        configuredStore: Store = Store.PLAY_STORE,
    ): Offerings {
        log(LogIntent.DEBUG) { OfferingStrings.BUILDING_OFFERINGS.format(productsById.size) }

        val jsonOfferings = offeringsJson.getJSONArray("offerings")
        val currentOfferingID = offeringsJson.getString("current_offering_id")

        val uiConfigJson = offeringsJson.optJSONObject("ui_config")

        @Suppress("TooGenericExceptionCaught")
        val uiConfig: UiConfig? = uiConfigJson?.let {
            try {
                json.decodeFromString<UiConfig>(it.toString())
            } catch (e: Throwable) {
                errorLog(e) { "Error deserializing ui_config" }
                null
            }
        }

        val offerings = mutableMapOf<String, Offering>()
        for (i in 0 until jsonOfferings.length()) {
            val offeringJson = jsonOfferings.getJSONObject(i)
            createOffering(offeringJson, productsById, uiConfig)?.let {
                offerings[it.identifier] = it

                if (it.availablePackages.isEmpty()) {
                    warnLog { offeringEmptyMessage(it.identifier, configuredStore) }
                }
            }
        }

        val targeting: Offerings.Targeting? = offeringsJson.optJSONObject("targeting")?.let {
            val revision = it.optNullableInt("revision")
            val ruleId = it.optNullableString("rule_id")

            return@let if (revision != null && ruleId != null) {
                Offerings.Targeting(revision, ruleId)
            } else {
                warnLog { OfferingStrings.TARGETING_ERROR }
                null
            }
        }

        val placements: Offerings.Placements? = offeringsJson.optJSONObject("placements")?.let {
            val fallbackOfferingId = it.getNullableString("fallback_offering_id")
            val offeringIdsByPlacement = it.optJSONObject("offering_ids_by_placement")
                ?.toMap<String?>()
                ?.replaceJsonNullWithKotlinNull()
                ?: emptyMap()

            Offerings.Placements(
                fallbackOfferingId = fallbackOfferingId,
                offeringIdsByPlacement = offeringIdsByPlacement,
            )
        }

        return Offerings(
            current = offerings[currentOfferingID]?.withPresentedContext(null, targeting),
            all = offerings,
            placements = placements,
            targeting = targeting,
            originalSource = originalSource,
            loadedFromDiskCache = loadedFromDiskCache,
        )
    }

    private fun offeringEmptyMessage(offeringIdentifier: String, configuredStore: Store): String {
        val template = if (configuredStore == Store.TEST_STORE) {
            OfferingStrings.OFFERING_EMPTY_TEST_STORE
        } else {
            OfferingStrings.OFFERING_EMPTY
        }
        return template.format(offeringIdentifier)
    }

    @OptIn(InternalRevenueCatAPI::class)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createOffering(
        offeringJson: JSONObject,
        productsById: Map<String, List<StoreProduct>>,
        uiConfig: UiConfig?,
    ): Offering? {
        val offeringIdentifier = offeringJson.getString("identifier")
        val metadata = offeringJson.optJSONObject("metadata")?.toMap<Any>(deep = true) ?: emptyMap()
        val jsonPackages = offeringJson.getJSONArray("packages")
        val presentedOfferingContext = PresentedOfferingContext(offeringIdentifier)

        val availablePackages = mutableListOf<Package>()
        for (i in 0 until jsonPackages.length()) {
            val packageJson = jsonPackages.getJSONObject(i)
            createPackage(packageJson, productsById, presentedOfferingContext)?.let {
                availablePackages.add(it)
            }
        }

        val paywallDataJson = offeringJson.optJSONObject("paywall")

        @Suppress("TooGenericExceptionCaught")
        val paywallData: PaywallData? = paywallDataJson?.let {
            try {
                json.decodeFromString<PaywallData>(it.toString())
            } catch (e: Exception) {
                errorLog(e) { "Error deserializing paywall data" }
                null
            }
        }

        val paywallComponentsJson = offeringJson.optJSONObject("paywall_components")
        // Presence is tracked independently of whether we decode: [Offering.hasPaywall] must keep reporting a
        // components paywall even when we skip capturing it (workflows serve it), so external integrators still
        // see the offering as paywall-capable.
        val hasPaywallComponents = hasWellShapedPaywallComponents(paywallComponentsJson, uiConfig)
        val paywallComponents = createPaywallComponents(paywallComponentsJson, uiConfig, hasPaywallComponents)

        val webCheckoutURL = offeringJson.getWebCheckoutURL()

        return if (availablePackages.isNotEmpty()) {
            Offering(
                offeringIdentifier,
                offeringJson.getString("description"),
                metadata,
                availablePackages,
                paywallData,
                paywallComponents,
                webCheckoutURL,
            ).also { it.hasPaywallComponents = hasPaywallComponents }
        } else {
            null
        }
    }

    /** Whether the backend sent a `paywall_components` object with the required shape for the given [uiConfig]. */
    @OptIn(InternalRevenueCatAPI::class)
    private fun hasWellShapedPaywallComponents(paywallComponentsJson: JSONObject?, uiConfig: UiConfig?): Boolean =
        paywallComponentsJson != null && uiConfig != null && paywallComponentsJson.hasPaywallComponentsShape()

    /**
     * Builds the (lazily-decoded) [Offering.PaywallComponents] from the raw JSON, or `null` when there is nothing
     * to build ([hasWellShaped] is false) or when [shouldParsePaywallComponents] says to skip capturing it.
     */
    @OptIn(InternalRevenueCatAPI::class)
    @Suppress("ReturnCount")
    private fun createPaywallComponents(
        paywallComponentsJson: JSONObject?,
        uiConfig: UiConfig?,
        hasWellShaped: Boolean,
    ): Offering.PaywallComponents? {
        if (paywallComponentsJson == null || uiConfig == null) return null
        if (!hasWellShaped) {
            warnLog { "Skipping paywall components data with unexpected shape for offering" }
            return null
        }
        if (!shouldParsePaywallComponents()) return null

        // Defer the (potentially expensive) component-tree deserialization until the paywall is actually
        // accessed/displayed. Capturing the raw JSON string here is cheap; without this we would eagerly
        // deserialize every cached offering's component tree at load, even those that are never shown.
        val rawPaywallComponents = paywallComponentsJson.toString()
        // A content hash of the raw JSON serves as the equality key, so comparing offerings (e.g. cached vs
        // network) never forces the lazy decode.
        return Offering.PaywallComponents(uiConfig, componentsHash = rawPaywallComponents.sha256()) {
            json.decodeFromString<PaywallComponentsData>(rawPaywallComponents)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createPackage(
        packageJson: JSONObject,
        productsById: Map<String, List<StoreProduct>>,
        presentedOfferingContext: PresentedOfferingContext,
    ): Package? {
        val packageIdentifier = packageJson.getString("identifier")
        val product = findMatchingProduct(productsById, packageJson)

        val packageType = packageIdentifier.toPackageType()

        val webCheckoutURL = packageJson.getWebCheckoutURL()

        return product?.let {
            Package(
                packageIdentifier,
                packageType,
                product.copyWithPresentedOfferingContext(presentedOfferingContext),
                presentedOfferingContext,
                webCheckoutURL,
            )
        }
    }
}

private fun JSONObject.getWebCheckoutURL(): URL? =
    this.optString("web_checkout_url").takeUnless { it.isNullOrEmpty() }?.let { urlString ->
        try {
            URL(urlString)
        } catch (e: MalformedURLException) {
            errorLog(e) { "Error parsing web checkout URL: $urlString" }
            null
        }
    }

private fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM

/**
 * Cheap structural check that a `paywall_components` object has the required top-level keys, without
 * deserializing the (potentially large) component tree. Mirrors the required fields of `PaywallComponentsData`,
 * so an obviously-malformed object is treated as "no paywall" at parse time (as before). Deeper structural
 * problems surface when the tree is lazily decoded and are handled by the paywall presentation layer.
 */
private fun JSONObject.hasPaywallComponentsShape(): Boolean =
    has("template_name") &&
        has("asset_base_url") &&
        has("components_config") &&
        has("components_localizations") &&
        has("default_locale")
