package com.revenuecat.purchases.ads

import dev.drewhamilton.poko.Poko
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Rewards configured for an ad unit, before presentation or verification.
 * Unknown or invalid individual rewards use [AdRewardConfiguration.UnsupportedReward].
 * A null [reward] means configuration was resolved successfully and no reward is configured for the ad unit.
 * Configured rewards describe what an app can advertise; they do not establish that a reward was granted.
 *
 * @property allRewards All rewards in configured order, primary first. Empty when no rewards are configured.
 */
@Poko
internal class ConfiguredAdRewards internal constructor(
    public val allRewards: List<AdRewardConfiguration>,
) {
    /** The primary reward, or null when no reward is configured. */
    public val reward: AdRewardConfiguration?
        get() = allRewards.firstOrNull()

    /** Additional rewards in configured order, excluding [reward]. Empty when there are no additional rewards. */
    public val moreRewards: List<AdRewardConfiguration>
        get() = allRewards.drop(1)
}

/** A reward configured before ad presentation. Unknown or invalid reward shapes use [UnsupportedReward]. */
internal interface AdRewardConfiguration {
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
        /** The positive duration in whole seconds for which the entitlement will be granted. */
        @get:JvmSynthetic
        public val duration: Duration,
    ) : AdRewardConfiguration {
        init {
            require(identifier.isNotEmpty() && duration.isPositive() && duration.isFinite())
            require(duration == duration.inWholeSeconds.seconds)
        }

        /** The configured duration in seconds, also available to Java callers. */
        public val durationInSeconds: Long
            get() = duration.inWholeSeconds
    }

    /** A configured reward with an unknown type or invalid values that this SDK version cannot model. */
    public object UnsupportedReward : AdRewardConfiguration
}
