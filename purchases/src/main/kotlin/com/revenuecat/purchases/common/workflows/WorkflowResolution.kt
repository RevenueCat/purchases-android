@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Outcome of resolving an offering to its workflow through the `/v1/config` workflows topic. It lets the paywall
 * render path tell apart four situations that a bare `workflowId?` conflates, so each can recover differently
 * without the caller needing to inspect remote-config state directly:
 *
 * - [Found]: the offering maps to a workflow id, which should be served through the workflows path.
 * - [NoWorkflow]: the workflows topic was readable and the offering genuinely has no workflow. This is a
 *   workflowless offering that should render its regular or default paywall.
 * - [Disabled]: the topic could not be read because the `/v1/config` endpoint is disabled for the session by a
 *   4xx kill switch. The offering was parsed with its paywall components skipped while workflows were enabled,
 *   so the caller should reload offerings — which now re-parse with those components — to recover its paywall.
 * - [Unavailable]: the topic could not be read for some other (transient) reason, so whether the offering has a
 *   workflow is unknown and reloading would not recover anything. The caller should surface an error.
 */
@InternalRevenueCatAPI
public sealed class WorkflowResolution {
    @InternalRevenueCatAPI
    public data class Found(val workflowId: String) : WorkflowResolution()

    @InternalRevenueCatAPI
    public object NoWorkflow : WorkflowResolution()

    @InternalRevenueCatAPI
    public object Disabled : WorkflowResolution()

    @InternalRevenueCatAPI
    public object Unavailable : WorkflowResolution()
}
