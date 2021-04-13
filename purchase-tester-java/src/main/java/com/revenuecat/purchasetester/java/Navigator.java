package com.revenuecat.purchasetester.java;

import android.content.Context;
import android.content.Intent;

public class Navigator {

    static void startCatsActivity(Context context, boolean clearBackStack) {
        Intent intent = new Intent(context, CatsActivity.class);
        if (clearBackStack) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    static void startUpsellActivity(Context context, boolean clearBackStack) {
        Intent intent = new Intent(context, UpsellActivity.class);
        if (clearBackStack) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

}
