package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.models.StoreTransaction;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateCallback;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingCompletion;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointOfferingPresenter;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointsExtensionsKt;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.EntitlementGrant;

import java.util.List;

@SuppressWarnings({"unused"})
final class CheckpointsAPI {

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void check(Purchases purchases, CheckpointParams params) {
        CheckpointGateCallback callback = (CheckpointGateResult gateResult) -> {
            List<EntitlementGrant> entitlements = gateResult.getEntitlements();
            CheckpointGateResult.NoActionReason noActionReason = gateResult.getNoActionReason();
            PurchasesError error = gateResult.getError();
        };
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", callback);
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", params, callback);
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkGrants(EntitlementGrant entitlementGrant) {
        String identifier = entitlementGrant.getIdentifier();
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkOfferingPresenter(
            Purchases purchases,
            CustomerInfo customerInfo,
            StoreTransaction storeTransaction,
            PurchasesError error
    ) {
        CheckpointOfferingPresenter presenter = (Offering offering, CheckpointOfferingCompletion completion) -> {
            completion.dismissed();
            completion.purchased(customerInfo, storeTransaction);
            completion.restored(customerInfo);
            completion.failed(error);
        };
        CheckpointsExtensionsKt.setCheckpointOfferingPresenter(purchases, presenter);
        CheckpointsExtensionsKt.setCheckpointOfferingPresenter(purchases, null);
        CheckpointOfferingPresenter current = CheckpointsExtensionsKt.getCheckpointOfferingPresenter(purchases);
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkNoActionReason() {
        CheckpointGateResult.NoActionReason noMatch = CheckpointGateResult.NoActionReason.NO_MATCH;
        CheckpointGateResult.NoActionReason holdout = CheckpointGateResult.NoActionReason.HOLDOUT;
        CheckpointGateResult.NoActionReason frequencyCapped =
                CheckpointGateResult.NoActionReason.FREQUENCY_CAPPED;
        CheckpointGateResult.NoActionReason configurationUnavailable =
                CheckpointGateResult.NoActionReason.CONFIGURATION_UNAVAILABLE;
        CheckpointGateResult.NoActionReason unknownCheckpoint =
                CheckpointGateResult.NoActionReason.UNKNOWN_CHECKPOINT;
        CheckpointGateResult.NoActionReason invalidCheckpointIdentifier =
                CheckpointGateResult.NoActionReason.INVALID_CHECKPOINT_IDENTIFIER;
        CheckpointGateResult.NoActionReason error = CheckpointGateResult.NoActionReason.ERROR;
    }
}
