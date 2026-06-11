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

internal abstract class OfferingParser {

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
        val paywallComponents = if (
            paywallComponentsJson != null &&
            uiConfig != null &&
            paywallComponentsJson.hasPaywallComponentsShape()
        ) {
            // Defer the (potentially expensive) component-tree deserialization until the paywall is actually
            // accessed/displayed. Capturing the raw JSON string here is cheap; without this we would eagerly
            // deserialize every cached offering's component tree at load, even those that are never shown.
            val rawPaywallComponents = paywallComponentsJson.toString()
            Offering.PaywallComponents(uiConfig) {
                json.decodeFromString<PaywallComponentsData>(rawPaywallComponents)
            }
        } else {
            if (paywallComponentsJson != null && uiConfig != null) {
                warnLog { "Skipping paywall components data with unexpected shape for offering" }
            }
            null
        }

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
            )
        } else {
            null
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
