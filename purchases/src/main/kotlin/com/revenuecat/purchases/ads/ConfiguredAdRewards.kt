package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import dev.drewhamilton.poko.Poko
import kotlin.time.Duration

/**
 * Rewards configured for an ad unit, before presentation or verification.
 * Unavailable configuration or invalid JSON/container structure is reported as an error.
 * Unknown or invalid individual rewards use [AdRewardConfiguration.UnsupportedReward].
 * A null [reward] means configuration was resolved successfully and no reward is configured for the ad unit.
 * Configured rewards describe what an app can advertise; they do not establish that a reward was granted.
 *
 * @property reward The primary reward, or null when no reward is configured.
 * @property moreRewards Additional rewards in configured order. Empty when [reward] is null.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
public class ConfiguredAdRewards internal constructor(
    public val reward: AdRewardConfiguration?,
    public val moreRewards: List<AdRewardConfiguration>,
) {
    init {
        require(reward != null || moreRewards.isEmpty())
    }
}

/** A reward configured before ad presentation. Unknown or invalid reward shapes use [UnsupportedReward]. */
@ExperimentalPreviewRevenueCatPurchasesAPI
public interface AdRewardConfiguration {
    /** A configured virtual currency reward. */
    @Poko
    public class VirtualCurrency internal constructor(
        /** The virtual currency code defined in the RevenueCat dashboard. */
        public val code: String,
        /** The positive configured amount. */
        public val amount: Int,
    ) : AdRewardConfiguration {
        init {
            require(code.isNotEmpty() && amount > 0)
        }
    }

    /** A configured entitlement reward. This describes a duration, rather than a granted expiration date. */
    @Poko
    public class Entitlement internal constructor(
        /** The entitlement identifier. */
        public val identifier: String,
        /** The positive duration for which the entitlement will be granted. */
        @get:JvmSynthetic
        public val duration: Duration,
    ) : AdRewardConfiguration {
        init {
            require(identifier.isNotEmpty() && duration.isPositive() && duration.isFinite())
        }

        /** The configured duration in seconds, also available to Java callers. */
        public val durationInSeconds: Long
            get() = duration.inWholeSeconds
    }

    /** A configured reward with an unknown type or invalid values that this SDK version cannot model. */
    public object UnsupportedReward : AdRewardConfiguration
}
