package com.revenuecat.apitester.java.revenuecatui;

import android.content.Context;
import android.util.AttributeSet;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI;
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener;
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider;
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallView;

@SuppressWarnings({"unused"})
final class PaywallViewAPI {

    static void checkConstructors(Context context,
                                  AttributeSet attrs,
                                  int defStyleAttr,
                                  Offering offering,
                                  PaywallListener listener,
                                  FontProvider fontProvider,
                                  boolean shouldDisplayDismissButton) {
        PaywallView paywallView = new PaywallView(context);
        PaywallView paywallView2 = new PaywallView(context, attrs);
        PaywallView paywallView3 = new PaywallView(context, attrs, defStyleAttr);
        PaywallView paywallView4 = new PaywallView(context, offering);
        PaywallView paywallView5 = new PaywallView(context, offering, listener);
        PaywallView paywallView6 = new PaywallView(context, offering, listener, fontProvider);
        PaywallView paywallView7 = new PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton);
        PaywallView paywallView8 = new PaywallView(context, offering, listener, fontProvider, shouldDisplayDismissButton, () -> null);
    }

    static void checkMethods(PaywallView paywallView, PaywallListener listener) {
        paywallView.setPaywallListener(null);
        paywallView.setPaywallListener(listener);
        paywallView.setDismissHandler(null);
        paywallView.setDismissHandler(() -> null);
        paywallView.setOfferingId(null);
        paywallView.setOfferingId("offeringId");
    }
}
