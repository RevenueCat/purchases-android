package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.InternalRevenueCatAPI;
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;

import java.util.Map;

@SuppressWarnings({"unused"})
final class CheckpointParamsAPI {

    @OptIn(markerClass = InternalRevenueCatAPI.class)
    static void check(CheckpointParams params) {
        Map<String, CustomVariableValue> customVariables = params.getCustomVariables();

        CheckpointParams empty = new CheckpointParams.Builder().build();
        Map<String, CustomVariableValue> added = new CheckpointParams.CustomVariablesBuilder()
                .add("string", "api-tester")
                .add("int", 1)
                .add("long", 1L)
                .add("double", 1.0)
                .add("float", 1.0f)
                .add("boolean", true)
                .add("value", new CustomVariableValue.Boolean(true))
                .build();
        CheckpointParams fromAdded = new CheckpointParams.Builder()
                .setCustomVariables(added)
                .build();
    }
}
