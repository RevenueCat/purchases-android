package com.revenuecat.apitester.java.revenuecatui;

import android.graphics.Color;

import com.revenuecat.purchases.paywalls.PaywallColor;

@SuppressWarnings({"unused"})
final class PaywallColorAPI {
    static void check(PaywallColor paywallColor) {
        String stringRepresentation = paywallColor.getStringRepresentation();
        Color underlyingColor = paywallColor.getUnderlyingColor();
        Integer colorInt = paywallColor.getColorInt();
    }

    static void checkConstructor(Integer colorInt) {
        PaywallColor paywallColor = new PaywallColor("#FFFFFF");
        PaywallColor paywallColor2 = new PaywallColor(colorInt);
    }
}
