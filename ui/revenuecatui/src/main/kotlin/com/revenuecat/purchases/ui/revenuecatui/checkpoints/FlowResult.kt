package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko

/**
 * What the user obtained while going through the flow a checkpoint presented. Delivered to
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint]'s callback, which receives null instead when
 * nothing was presented.
 */
@InternalRevenueCatAPI
@Poko
public class FlowResult internal constructor(
    /** The entitlements the user obtained during this checkpoint. Empty when they obtained none. */
    public val obtainedEntitlements: Set<ObtainedEntitlement>,
)

/**
 * An entitlement the user obtained during a checkpoint.
 */
@InternalRevenueCatAPI
@Poko
public class ObtainedEntitlement internal constructor(
    /** The entitlement as it stands after the checkpoint. */
    public val entitlementInfo: EntitlementInfo,
)
