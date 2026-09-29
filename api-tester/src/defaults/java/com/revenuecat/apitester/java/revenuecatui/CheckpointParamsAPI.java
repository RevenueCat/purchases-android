package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI;
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams;
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPresentationMode;

import java.util.Map;

@SuppressWarnings({"unused"})
final class CheckpointParamsAPI {

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void check(CheckpointParams params) {
        Map<String, CustomVariableValue> customVariables = params.getCustomVariables();
        CheckpointPresentationMode presentationMode = params.getPresentationMode();

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
        CheckpointParams sheet = new CheckpointParams.Builder()
                .setPresentationMode(CheckpointPresentationMode.SHEET)
                .build();
    }

    @OptIn(markerClass = InviteOnlyCheckpointsAPI.class)
    static void checkPresentationMode(CheckpointPresentationMode mode) {
        CheckpointPresentationMode defaultMode = CheckpointPresentationMode.DEFAULT;
        CheckpointPresentationMode fullScreen = CheckpointPresentationMode.FULL_SCREEN;
        CheckpointPresentationMode sheet = CheckpointPresentationMode.SHEET;
        boolean same = mode.equals(defaultMode);
        int hash = mode.hashCode();
        String text = mode.toString();
    }
}
