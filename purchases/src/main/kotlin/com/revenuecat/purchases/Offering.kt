//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import dev.drewhamilton.poko.Poko
import java.net.URL

/**
 * An offering is a collection of [Package] available for the user to purchase.
 * For more info see https://docs.revenuecat.com/docs/entitlements
 * @property identifier Unique identifier defined in RevenueCat dashboard.
 * @property serverDescription Offering description defined in RevenueCat dashboard.
 * @property availablePackages Array of [Package] objects available for purchase.
 * @property metadata Offering metadata defined in RevenueCat dashboard.
 * @property webCheckoutURL If the Offering has an associated Web Purchase Link, this will be the URL for it.
 */
@Suppress("UnsafeOptInUsageError")
@Poko
public class Offering
@OptIn(InternalRevenueCatAPI::class)
constructor(
    public val identifier: String,
    public val serverDescription: String,
    public val metadata: Map<String, Any>,
    public val availablePackages: List<Package>,
    @InternalRevenueCatAPI
    public val paywall: PaywallData? = null,
    @InternalRevenueCatAPI
    public val paywallComponents: PaywallComponents? = null,
    public val webCheckoutURL: URL? = null,
) {
    @OptIn(InternalRevenueCatAPI::class)
    public constructor(
        identifier: String,
        serverDescription: String,
        metadata: Map<String, Any>,
        availablePackages: List<Package>,
    ) : this(
        identifier = identifier,
        serverDescription = serverDescription,
        metadata = metadata,
        availablePackages = availablePackages,
        paywall = null,
        paywallComponents = null,
        webCheckoutURL = null,
    )

    @InternalRevenueCatAPI
    @Poko
    public class PaywallComponents(
        public val uiConfig: UiConfig,
        public val data: PaywallComponentsData,
    )

    /**
     * Whether the offering contains a paywall.
     */
    @OptIn(InternalRevenueCatAPI::class)
    @get:JvmName("hasPaywall")
    public val hasPaywall: Boolean
        get() = paywall != null || paywallComponents != null

    /**
     * Lifetime package type configured in the RevenueCat dashboard, if available.
     */
    public val lifetime: Package? by lazy { findPackage(PackageType.LIFETIME) }

    /**
     * Annual package type configured in the RevenueCat dashboard, if available.
     */
    public val annual: Package? by lazy { findPackage(PackageType.ANNUAL) }

    /**
     * Six month package type configured in the RevenueCat dashboard, if available.
     */
    public val sixMonth: Package? by lazy { findPackage(PackageType.SIX_MONTH) }

    /**
     * Three month package type configured in the RevenueCat dashboard, if available.
     */
    public val threeMonth: Package? by lazy { findPackage(PackageType.THREE_MONTH) }

    /**
     * Two month package type configured in the RevenueCat dashboard, if available.
     */
    public val twoMonth: Package? by lazy { findPackage(PackageType.TWO_MONTH) }

    /**
     * Monthly package type configured in the RevenueCat dashboard, if available.
     */
    public val monthly: Package? by lazy { findPackage(PackageType.MONTHLY) }

    /**
     * Weekly package type configured in the RevenueCat dashboard, if available.
     */
    public val weekly: Package? by lazy { findPackage(PackageType.WEEKLY) }

    private fun findPackage(packageType: PackageType) =
        availablePackages.firstOrNull { it.identifier == packageType.identifier }

    /**
     * Retrieves a specific package by identifier, use this to access custom package types configured
     * in the RevenueCat dashboard. Equivalent to calling `getPackage`.
     */
    public operator fun get(s: String): Package = getPackage(s)

    /**
     * Retrieves a specific package by identifier, use this to access custom package types configured
     * in the RevenueCat dashboard
     * @throws NoSuchElementException if there's no package with the specified identifier in the Offering.
     */
    @Throws(NoSuchElementException::class)
    @Suppress("MemberVisibilityCanBePrivate")
    public fun getPackage(identifier: String): Package =
        availablePackages.first { it.identifier == identifier }

    /**
     * Returns the `metadata` value associated to `key` for the expected `String` type
     * or `default` if not found, or it's not the expected `String` type.
     */
    public fun getMetadataString(key: String, default: String): String {
        return this.metadata[key] as? String ?: default
    }

    @InternalRevenueCatAPI
    public fun copy(presentedOfferingContext: PresentedOfferingContext): Offering {
        return Offering(
            identifier = this.identifier,
            serverDescription = this.serverDescription,
            metadata = this.metadata,
            availablePackages = this.availablePackages.map { it.copy(presentedOfferingContext) },
            paywall = this.paywall,
            paywallComponents = this.paywallComponents,
            webCheckoutURL = this.webCheckoutURL,
        )
    }
}
