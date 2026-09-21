package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPassedCallback;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointsExtensionsKt;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedEntitlement;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused"})
final class CheckpointsAPI {

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void check(Purchases purchases, CheckpointParams params) {
        CheckpointPassedCallback callback = (FlowResult result) -> {
            if (result != null) {
                Set<ObtainedEntitlement> obtainedEntitlements = result.getObtainedEntitlements();
            }
        };
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", callback);
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", params, callback);
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkParams(PaywallPresenter presenter, ErrorPresenter errorPresenter) {
        CheckpointParams params = new CheckpointParams.Builder()
                .setCustomVariables(Collections.singletonMap("key", new CustomVariableValue.String("value")))
                .setPaywallPresenter(presenter)
                .setPaywallPresenter(null)
                .setErrorPresenter(errorPresenter)
                .setErrorPresenter(null)
                .build();
        Map<String, CustomVariableValue> customVariables = params.getCustomVariables();
        PaywallPresenter paywallPresenter = params.getPaywallPresenter();
        ErrorPresenter currentErrorPresenter = params.getErrorPresenter();
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkObtainedEntitlement(ObtainedEntitlement obtainedEntitlement) {
        EntitlementInfo entitlementInfo = obtainedEntitlement.getEntitlementInfo();
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkPaywallPresenter(Purchases purchases) {
        PaywallPresenter presenter = (PaywallPresenter.Params params, PaywallPresenter.Completion completion) -> {
            Offering offering = params.getOffering();
            String checkpointIdentifier = params.getCheckpointIdentifier();
            Map<String, CustomVariableValue> customVariables = params.getCustomVariables();
            completion.complete(PaywallPresenter.Completion.Result.Continued.INSTANCE);
            completion.complete(PaywallPresenter.Completion.Result.Closed.INSTANCE);
            completion.complete(PaywallPresenter.Completion.Result.NavigatedBack.INSTANCE);
        };
        CheckpointsExtensionsKt.setPaywallPresenter(purchases, presenter);
        CheckpointsExtensionsKt.setPaywallPresenter(purchases, null);
        PaywallPresenter current = CheckpointsExtensionsKt.getPaywallPresenter(purchases);
    }

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void checkErrorPresenter(Purchases purchases) {
        ErrorPresenter presenter = (ErrorPresenter.Params params, ErrorPresenter.Completion completion) -> {
            PurchasesError error = params.getError();
            String checkpointIdentifier = params.getCheckpointIdentifier();
            Map<String, CustomVariableValue> customVariables = params.getCustomVariables();
            boolean flowCanContinue = params.getFlowCanContinue();
            completion.complete(ErrorPresenter.Completion.Result.Retry.INSTANCE);
            completion.complete(ErrorPresenter.Completion.Result.Continued.INSTANCE);
            completion.complete(ErrorPresenter.Completion.Result.NavigatedBack.INSTANCE);
        };
        CheckpointsExtensionsKt.setErrorPresenter(purchases, presenter);
        CheckpointsExtensionsKt.setErrorPresenter(purchases, null);
        ErrorPresenter current = CheckpointsExtensionsKt.getErrorPresenter(purchases);
    }
}
