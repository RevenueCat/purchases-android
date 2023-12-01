package com.revenuecat.apitester.java.revenuecatui;

import android.content.Context;
import android.util.AttributeSet;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI;
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener;
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider;
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallFooterView;

@SuppressWarnings({"unused"})
@ExperimentalPreviewRevenueCatUIPurchasesAPI
final class PaywallFooterViewAPI {

    static void checkConstructors(Context context,
                                  AttributeSet attrs,
                                  int defStyleAttr,
                                  Offering offering,
                                  PaywallListener listener,
                                  FontProvider fontProvider ) {
        PaywallFooterView footerView = new PaywallFooterView(context);
        PaywallFooterView footerView2 = new PaywallFooterView(context, attrs);
        PaywallFooterView footerView3 = new PaywallFooterView(context, attrs, defStyleAttr);
        PaywallFooterView footerView4 = new PaywallFooterView(context, offering);
        PaywallFooterView footerView5 = new PaywallFooterView(context, offering, listener);
        PaywallFooterView footerView6 = new PaywallFooterView(context, offering, listener, fontProvider);
        PaywallFooterView footerView7 = new PaywallFooterView(context, offering, listener, fontProvider, () -> null);
    }

    static void checkMethods(PaywallFooterView footerView, PaywallListener listener) {
        footerView.setPaywallListener(null);
        footerView.setPaywallListener(listener);
        footerView.setDismissHandler(null);
        footerView.setDismissHandler(() -> null);
        footerView.setOfferingId(null);
        footerView.setOfferingId("offeringId");
    }
}
