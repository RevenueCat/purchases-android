package com.revenuecat.purchases.common

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.api.BuildConfig
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
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal abstract class OfferingParser {

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val json = Json {
            ignoreUnknownKeys = true
        }
    }

    protected abstract fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct?

    /**
     * Note: this may return an empty Offerings.
     */
    fun createOfferings(offeringsJson: JSONObject, productsById: Map<String, List<StoreProduct>>): Offerings {
        log(LogIntent.DEBUG, OfferingStrings.BUILDING_OFFERINGS.format(productsById.size))

        val jsonOfferings = offeringsJson.getJSONArray("offerings")
        val currentOfferingID = offeringsJson.getString("current_offering_id")

        val offerings = mutableMapOf<String, Offering>()
        for (i in 0 until jsonOfferings.length()) {
            val offeringJson = jsonOfferings.getJSONObject(i)
            createOffering(offeringJson, productsById)?.let {
                offerings[it.identifier] = it

                if (it.availablePackages.isEmpty()) {
                    warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
                }
            }
        }

        val targeting: Offerings.Targeting? = offeringsJson.optJSONObject("targeting")?.let {
            val revision = it.optNullableInt("revision")
            val ruleId = it.optNullableString("rule_id")

            return@let if (revision != null && ruleId != null) {
                Offerings.Targeting(revision, ruleId)
            } else {
                warnLog(OfferingStrings.TARGETING_ERROR)
                null
            }
        }

        val placements: Offerings.Placements? = offeringsJson.optJSONObject("placements")?.let {
            val fallbackOfferingId = it.getNullableString("fallback_offering_id")
            val offeringIdsByPlacement = it.optJSONObject("offering_ids_by_placement")
                ?.toMap<String?>()
                ?.replaceJsonNullWithKotlinNull()

            return@let offeringIdsByPlacement?.let {
                Offerings.Placements(
                    fallbackOfferingId = fallbackOfferingId,
                    offeringIdsByPlacement = offeringIdsByPlacement,
                )
            } ?: run {
                null
            }
        }

        return Offerings(
            current = offerings[currentOfferingID]?.withPresentedContext(null, targeting),
            all = offerings,
            placements = placements,
            targeting = targeting,
        )
    }

    @OptIn(InternalRevenueCatAPI::class)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun createOffering(offeringJson: JSONObject, productsById: Map<String, List<StoreProduct>>): Offering? {
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
                errorLog("Error deserializing paywall data", e)
                null
            }
        }

        @Suppress("TooGenericExceptionCaught")
        val paywallComponentsData: PaywallComponentsData? =
            if (BuildConfig.FLAG_PAYWALL_COMPONENTS) {
                offeringJson.optJSONObject("paywall_components")?.let {
                    try {
                        json.decodeFromString<PaywallComponentsData>(it.toString())
                    } catch (e: Throwable) {
                        errorLog("Error deserializing paywall components data", e)
                        null
                    }
                }
            } else {
                null
            }

        return if (availablePackages.isNotEmpty()) {
            Offering(
                offeringIdentifier,
                offeringJson.getString("description"),
                metadata,
                availablePackages,
                paywallData,
                paywallComponentsData,
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
        return product?.let {
            Package(
                packageIdentifier,
                packageType,
                product.copyWithPresentedOfferingContext(presentedOfferingContext),
                presentedOfferingContext,
            )
        }
    }
}

private fun String.toPackageType(): PackageType =
    PackageType.values().firstOrNull { it.identifier == this }
        ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM
