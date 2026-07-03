package com.revenuecat.purchases.ads.rewardverification

import kotlinx.coroutines.delay

internal const val ENTITLEMENT_REFRESH_RETRY_DELAY_MS = 300L

/**
 * Pause between CustomerInfo-refresh retries after an entitlement grant. Mirrors iOS `Async.retry`'s
 * 300ms `pollInterval` so a brief connectivity blip doesn't exhaust all retries instantly and turn an
 * already-verified poll into a failed result. Lives here (not in `Purchases`) to keep coroutine plumbing
 * out of the public class.
 */
internal suspend fun rewardVerificationRetryDelay() {
    delay(ENTITLEMENT_REFRESH_RETRY_DELAY_MS)
}
