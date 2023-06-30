package com.revenuecat.purchases

/**
 * This class contains all the offerings configured in RevenueCat dashboard.
 * For more info see https://docs.revenuecat.com/docs/entitlements
 * @property current Current offering configured in the RevenueCat dashboard.
 * @property all Dictionary of all Offerings [Offering] objects keyed by their identifier.
 */
data class Offerings(
    val current: Offering?,
    val all: Map<String, Offering>,
) {

    /**
     * Retrieves an specific offering by its identifier.
     * @param identifier Offering identifier
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getOffering(identifier: String) = all[identifier]

    /**
     * Retrieves an specific offering by its identifier. It's equivalent to
     * calling [getOffering(identifier)]
     * @param identifier Offering identifier
     */
    operator fun get(identifier: String) = getOffering(identifier)
}
