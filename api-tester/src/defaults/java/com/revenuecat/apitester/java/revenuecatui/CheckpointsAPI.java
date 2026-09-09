package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointCallback;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointsExtensionsKt;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedEntitlement;

import java.util.Set;

@SuppressWarnings({"unused"})
final class CheckpointsAPI {

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void check(Purchases purchases, CheckpointParams params) {
        CheckpointCallback callback = (FlowResult result) -> {
            if (result != null) {
                Set<ObtainedEntitlement> obtainedEntitlements = result.getObtainedEntitlements();
            }
        };
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", callback);
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", params, callback);
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkObtainedEntitlement(ObtainedEntitlement obtainedEntitlement) {
        EntitlementInfo entitlementInfo = obtainedEntitlement.getEntitlementInfo();
    }
}
