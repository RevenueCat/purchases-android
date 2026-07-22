package com.revenuecat.purchases

/**
 * Test-only server-error injection for RevenueCat internal E2E tests. Internal RevenueCat use only;
 * behavior may change without warning.
 *
 * Set via [DangerousSettings.forWorkflows] to force failures on the remote-config endpoint so the
 * SDK's workflow fallback paths can be exercised end-to-end.
 */
@InternalRevenueCatAPI
public enum class ForceServerErrorMode {
    /**
     * Forces the remote-config endpoint to return not-found (4xx), so a workflow offering falls back
     * to its classic single-page paywall (the kill-switch path).
     */
    REMOTE_CONFIG_NOT_FOUND,

    /**
     * Routes remote-config requests to an unreachable host, simulating no network. With nothing cached
     * the SDK renders its default paywall; with a warm cache it renders the cached workflow.
     */
    REMOTE_CONFIG_NETWORK_ERROR,
}
