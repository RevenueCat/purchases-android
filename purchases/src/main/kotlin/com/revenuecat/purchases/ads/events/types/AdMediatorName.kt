package com.revenuecat.purchases.ads.events.types

/**
 * Common ad mediator names.
 */
@JvmInline
public value class AdMediatorName internal constructor(internal val value: String) {
    public companion object {
        public val AD_MOB: AdMediatorName = AdMediatorName("AdMob")
        public val APP_LOVIN: AdMediatorName = AdMediatorName("AppLovin")
        public val LEVEL_PLAY: AdMediatorName = AdMediatorName("LevelPlay")

        public fun fromString(value: String): AdMediatorName {
            return when (value.trim()) {
                "AdMob" -> AD_MOB
                "AppLovin" -> APP_LOVIN
                "LevelPlay" -> LEVEL_PLAY
                else -> AdMediatorName(value)
            }
        }
    }
}
