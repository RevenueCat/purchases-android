@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Outcome of resolving an offering to its workflow through the `/v1/config` workflows topic. It lets the paywall
 * render path tell apart three situations that a bare `workflowId?` conflates:
 *
 * - [Found]: the offering maps to a workflow id, which should be served through the workflows path.
 * - [NoWorkflow]: the workflows topic was readable and the offering genuinely has no workflow. This is a
 *   workflowless offering that should render its regular or default paywall.
 * - [Unresolved]: the workflows topic could not be read at all — the `/v1/config` endpoint is disabled by a 4xx
 *   kill switch, or a sync failed transiently — so whether the offering has a workflow is unknown. The caller
 *   decides how to recover based on whether remote config is disabled.
 */
@InternalRevenueCatAPI
public sealed class WorkflowResolution {
    @InternalRevenueCatAPI
    public data class Found(val workflowId: String) : WorkflowResolution()

    @InternalRevenueCatAPI
    public object NoWorkflow : WorkflowResolution()

    @InternalRevenueCatAPI
    public object Unresolved : WorkflowResolution()
}
