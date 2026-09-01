package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateCallback;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointsExtensionsKt;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.EntitlementGrant;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.GrantMethod;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.VirtualCurrencyGrant;

import java.util.List;

@SuppressWarnings({"unused"})
final class CheckpointsAPI {

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void check(Purchases purchases, CheckpointParams params) {
        CheckpointGateCallback callback = (CheckpointGateResult gateResult) -> {
            List<EntitlementGrant> entitlements = gateResult.getEntitlements();
            List<VirtualCurrencyGrant> virtualCurrencies = gateResult.getVirtualCurrencies();
            CheckpointGateResult.NoWorkflowReason noWorkflowReason = gateResult.getNoWorkflowReason();
            PurchasesError error = gateResult.getError();
        };
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", callback);
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", params, callback);
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkGrants(EntitlementGrant entitlementGrant, VirtualCurrencyGrant virtualCurrencyGrant) {
        String identifier = entitlementGrant.getIdentifier();
        GrantMethod entitlementMethod = entitlementGrant.getMethod();
        String code = virtualCurrencyGrant.getCode();
        int amount = virtualCurrencyGrant.getAmount();
        GrantMethod virtualCurrencyMethod = virtualCurrencyGrant.getMethod();
        GrantMethod purchased = GrantMethod.PURCHASED;
        GrantMethod restored = GrantMethod.RESTORED;
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkNoWorkflowReason() {
        CheckpointGateResult.NoWorkflowReason noMatch = CheckpointGateResult.NoWorkflowReason.NO_MATCH;
        CheckpointGateResult.NoWorkflowReason holdout = CheckpointGateResult.NoWorkflowReason.HOLDOUT;
        CheckpointGateResult.NoWorkflowReason frequencyCapped =
                CheckpointGateResult.NoWorkflowReason.FREQUENCY_CAPPED;
        CheckpointGateResult.NoWorkflowReason configurationUnavailable =
                CheckpointGateResult.NoWorkflowReason.CONFIGURATION_UNAVAILABLE;
        CheckpointGateResult.NoWorkflowReason unknownCheckpoint =
                CheckpointGateResult.NoWorkflowReason.UNKNOWN_CHECKPOINT;
        CheckpointGateResult.NoWorkflowReason invalidCheckpointIdentifier =
                CheckpointGateResult.NoWorkflowReason.INVALID_CHECKPOINT_IDENTIFIER;
        CheckpointGateResult.NoWorkflowReason error = CheckpointGateResult.NoWorkflowReason.ERROR;
    }
}
