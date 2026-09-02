package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Outcome of resolving an offering to its workflow through the `/v1/config` workflows topic. It lets the paywall
 * render path tell apart three situations that a bare `workflowId?` conflates, so each can recover differently
 * without the caller needing to inspect remote-config state directly:
 *
 * - [Found]: the offering maps to a workflow id, which should be served through the workflows path.
 * - [NoWorkflow]: the workflows topic was readable and the offering genuinely has no workflow. This is a
 *   workflowless offering that should render its regular or default paywall.
 * - [Unavailable]: the topic could not be read, so whether the offering has a workflow is unknown. The caller
 *   should fall back to the offering's default paywall, not surface an error.
 */
@InternalRevenueCatAPI
public sealed class WorkflowResolution {
    public data class Found(val workflowId: String) : WorkflowResolution()

    public object NoWorkflow : WorkflowResolution()

    public object Unavailable : WorkflowResolution()
}
