package com.revenuecat.purchases.common.remoteconfig

/**
 * The mutable state of one tree sync pass ([RemoteConfigManager.runTreePass]); touched only by the pass
 * coroutine (domains sync sequentially).
 */
internal class TreePassContext(
    val requestEpoch: Int,
    val appInBackground: Boolean,
    val appUserID: String,
    val fetchContext: RemoteConfigFetchContext,
    /** The root name this pass synced, for a child commit landing before any root state exists. */
    val rootDomainName: String,
) {
    /** Terminal outcome per synced domain; doubles as the "already synced this pass" dedupe. */
    val outcomes = mutableMapOf<String, DomainSyncOutcome>()

    /** Domains whose sync is on the recursion stack; a re-listing inside its own subtree is a cycle. */
    val inProgress = mutableSetOf<String>()

    /** Server refresh times confirmed by per-domain 204s, written in one batch at pass end. */
    val pending204RefreshTimes = mutableMapOf<String, Long>()

    var committedCount = 0

    /** The tree as of this pass's most recent successful commit; null when nothing committed yet. */
    var lastPersistedState: PersistedRemoteConfigurationState? = null

    val allFresh: Boolean get() = outcomes.values.all { it.isFresh }
}

internal enum class DomainSyncOutcome {
    Committed,
    Unchanged,
    Skipped,

    /** Refused (4xx) this session but its last-good persisted entry keeps serving the merged view. */
    DisabledLastGood,
    Failed,
    ;

    /** Whether the domain's subtree is safe to apply a parent's deletions against. */
    val isFresh: Boolean get() = this != Failed
}

/**
 * The tree after committing one domain's `200` [response] into its entry. Children stay keyed by
 * [domainKey] — the name their parent listed; only the root follows the response's echoed domain, and a root
 * rename replaces the whole tree (old entries would be unreachable stale state, not history).
 * [fallbackRootDomain] names the root when a child commits before any state exists.
 */
@Suppress("LongParameterList")
internal fun PersistedRemoteConfigurationState?.withCommittedDomain(
    domainKey: String,
    isRoot: Boolean,
    fallbackRootDomain: String,
    response: RemoteConfiguration,
    mergedTopics: Map<String, ConfigTopic>,
    lastRefreshTime: Long?,
): PersistedRemoteConfigurationState {
    val carriedDomains = when {
        !isRoot -> this?.domains ?: emptyMap()
        this?.rootDomain == response.domain -> this.domains
        else -> emptyMap()
    }
    return PersistedRemoteConfigurationState(
        rootDomain = if (isRoot) response.domain else this?.rootDomain ?: fallbackRootDomain,
        domains = carriedDomains + (
            domainKey to PersistedDomainState(
                manifest = response.manifest,
                subdomains = response.subdomains,
                activeTopics = response.activeTopics,
                prefetchBlobs = response.prefetchBlobs,
                topics = mergedTopics,
                lastRefreshTime = lastRefreshTime,
            )
            ),
    )
}
