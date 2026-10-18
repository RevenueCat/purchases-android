package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import dev.drewhamilton.poko.Poko

/**
 * What the user obtained while going through the flow a checkpoint presented. Delivered as
 * [CheckpointResult.flow], which is null instead when nothing was presented.
 */
@InviteOnlyCheckpointsAPI
@Poko
public class FlowResult internal constructor(
    /** The entitlements the user obtained during this checkpoint. Empty when they obtained none. */
    public val obtainedEntitlements: Set<ObtainedEntitlement>,
    /**
     * The products the user obtained during this checkpoint, among those the presented flow could sell: a product
     * whose purchase date is new or changed in the customer's information after the flow. Empty when they
     * obtained none.
     */
    public val obtainedProducts: Set<ObtainedProduct>,
) {

    /** The entitlement with [identifier] the user obtained during this checkpoint, or null. */
    public fun obtainedEntitlement(identifier: String): ObtainedEntitlement? =
        obtainedEntitlements.firstOrNull { it.entitlementInfo.identifier == identifier }

    /** Whether the user obtained the entitlement with [identifier] during this checkpoint. */
    public fun didObtainEntitlement(identifier: String): Boolean = obtainedEntitlement(identifier) != null

    /**
     * The product with [identifier] the user obtained during this checkpoint, or null. [identifier] is the store
     * product identifier: for a Play subscription, the subscription's product identifier without base plan.
     */
    public fun obtainedProduct(identifier: String): ObtainedProduct? =
        obtainedProducts.firstOrNull { it.product.purchasingData.productId == identifier }

    /** Whether the user obtained the product with [identifier] during this checkpoint. */
    public fun didObtainProduct(identifier: String): Boolean = obtainedProduct(identifier) != null
}

/**
 * An entitlement the user obtained during a checkpoint.
 */
@InviteOnlyCheckpointsAPI
@Poko
public class ObtainedEntitlement internal constructor(
    /** The entitlement as it stands after the checkpoint. */
    public val entitlementInfo: EntitlementInfo,
)

/**
 * A product the user obtained during a checkpoint. Two obtained products are the same when they share the store
 * product identifier ([StoreProduct.purchasingData]'s), whichever package presented them.
 */
@InviteOnlyCheckpointsAPI
@Poko
public class ObtainedProduct internal constructor(
    /** The product, as the presented flow offered it. */
    public val product: StoreProduct,
) {

    override fun equals(other: Any?): Boolean =
        other is ObtainedProduct && other.product.purchasingData.productId == product.purchasingData.productId

    override fun hashCode(): Int = product.purchasingData.productId.hashCode()
}
