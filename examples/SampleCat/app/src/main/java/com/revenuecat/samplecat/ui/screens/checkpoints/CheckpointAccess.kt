package com.revenuecat.samplecat.ui.screens.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.samplecat.config.Constants

/**
 * Whether the customer has access to premium content. Uses [Constants.entitlementId] when it is set, else any
 * active entitlement.
 */
internal val CustomerInfo.hasPremiumAccess: Boolean
    get() = Constants.entitlementId
        ?.let { entitlements[it]?.isActive == true }
        ?: entitlements.active.isNotEmpty()

/**
 * Whether the customer in the checkpoint result has access. It also covers a customer who already had access
 * before the checkpoint.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
internal val CheckpointResult.hasPremiumAccess: Boolean
    get() = Constants.entitlementId?.let(::hasEntitlement) ?: customer.hasPremiumAccess
