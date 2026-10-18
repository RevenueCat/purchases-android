package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPassedCallback;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointFailureKind;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowPresentationMode;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointsExtensionsKt;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedEntitlement;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ObtainedProduct;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused"})
final class CheckpointsAPI {

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void check(Purchases purchases, CheckpointParams params) {
        CheckpointPassedCallback callback = (CheckpointResult result) -> {
            FlowResult flow = result.getFlow();
            CustomerInfo customer = result.getCustomer();
            ObtainedEntitlement obtainedEntitlement = result.obtainedEntitlement("pro");
            boolean didObtainEntitlement = result.didObtainEntitlement("pro");
            ObtainedProduct obtainedProduct = result.obtainedProduct("monthly");
            boolean didObtainProduct = result.didObtainProduct("monthly");
            if (flow != null) {
                Set<ObtainedEntitlement> obtainedEntitlements = flow.getObtainedEntitlements();
                Set<ObtainedProduct> obtainedProducts = flow.getObtainedProducts();
                ObtainedEntitlement flowEntitlement = flow.obtainedEntitlement("pro");
                boolean flowDidObtainEntitlement = flow.didObtainEntitlement("pro");
                ObtainedProduct flowProduct = flow.obtainedProduct("monthly");
                boolean flowDidObtainProduct = flow.didObtainProduct("monthly");
            }
        };
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", callback);
        CheckpointsExtensionsKt.checkpoint(purchases, "checkpoint_identifier", params, callback);
    }

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
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

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void checkObtainedEntitlement(ObtainedEntitlement obtainedEntitlement) {
        EntitlementInfo entitlementInfo = obtainedEntitlement.getEntitlementInfo();
    }

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void checkObtainedProduct(ObtainedProduct obtained) {
        StoreProduct product = obtained.getProduct();
    }

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void checkPaywallPresenter(Purchases purchases) {
        PaywallPresenter presenter = (PaywallPresenter.Params params, PaywallPresenter.Completion completion) -> {
            Offering offering = params.getOffering();
            String checkpointIdentifier = params.getCheckpointIdentifier();
            Map<String, CustomVariableValue> customVariables = params.getCustomVariables();
            FlowPresentationMode presentationMode = params.getPresentationMode();
            completion.complete(PaywallPresenter.Completion.Result.Continued.INSTANCE);
            completion.complete(PaywallPresenter.Completion.Result.Closed.INSTANCE);
            completion.complete(PaywallPresenter.Completion.Result.NavigatedBack.INSTANCE);
        };
        CheckpointsExtensionsKt.setPaywallPresenter(purchases, presenter);
        CheckpointsExtensionsKt.setPaywallPresenter(purchases, null);
        PaywallPresenter current = CheckpointsExtensionsKt.getPaywallPresenter(purchases);
    }

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void checkErrorPresenter(Purchases purchases) {
        ErrorPresenter presenter = (ErrorPresenter.Params params, ErrorPresenter.Completion completion) -> {
            PurchasesError error = params.getError();
            String checkpointIdentifier = params.getCheckpointIdentifier();
            Map<String, CustomVariableValue> customVariables = params.getCustomVariables();
            CheckpointFailureKind kind = params.getKind();
            completion.complete();
        };
        CheckpointsExtensionsKt.setErrorPresenter(purchases, presenter);
        CheckpointsExtensionsKt.setErrorPresenter(purchases, null);
        ErrorPresenter current = CheckpointsExtensionsKt.getErrorPresenter(purchases);
    }

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void checkCheckpointFailureKind(CheckpointFailureKind kind) {
        CheckpointFailureKind before = CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN;
        CheckpointFailureKind during = CheckpointFailureKind.FLOW_ACTION_FAILED;
        CheckpointFailureKind after = CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED;
        boolean isBefore = kind.equals(before);
    }
}
