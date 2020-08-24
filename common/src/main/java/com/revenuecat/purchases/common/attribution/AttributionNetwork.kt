package com.revenuecat.purchases.common.attribution

/**
 * Equivalent to Purchases.AttributionNetwork. We can't remove that enum class because it's public
 */
@Suppress("unused", "MagicNumber")
enum class AttributionNetwork(val serverValue: Int) {
    /**
     * [https://www.adjust.com/]
     */
    ADJUST(1),

    /**
     * [https://www.appsflyer.com/]
     */
    APPSFLYER(2),

    /**
     * [http://branch.io/]
     */
    BRANCH(3),

    /**
     * [http://tenjin.io/]
     */
    TENJIN(4),

    /**
     * [https://developers.facebook.com/]
     */
    FACEBOOK(5),

    /**
     * [https://www.mparticle.com/]
     */
    MPARTICLE(6)
}
