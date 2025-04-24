package com.revenuecat.apitester.java.revenuecatui;

import android.content.Context;
import android.util.AttributeSet;

import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener;
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider;
import com.revenuecat.purchases.ui.revenuecatui.views.OriginalTemplatePaywallFooterView;
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallFooterView;

@SuppressWarnings({"unused"})
final class PaywallFooterViewAPI {

    static void checkConstructors(Context context,
                                  AttributeSet attrs,
                                  int defStyleAttr,
                                  Offering offering,
                                  PaywallListener listener,
                                  FontProvider fontProvider,
                                  Boolean condensed) {
        PaywallFooterView deprecatedFooterView = new PaywallFooterView(context);
        PaywallFooterView deprecatedFooterView2 = new PaywallFooterView(context, attrs);
        PaywallFooterView deprecatedFooterView3 = new PaywallFooterView(context, attrs, defStyleAttr);
        PaywallFooterView deprecatedFooterView4 = new PaywallFooterView(context, offering);
        PaywallFooterView deprecatedFooterView5 = new PaywallFooterView(context, offering, listener);
        PaywallFooterView deprecatedFooterView6 = new PaywallFooterView(context, offering, listener, fontProvider);
        PaywallFooterView deprecatedFooterView7 = new PaywallFooterView(context, offering, listener, fontProvider, condensed);
        PaywallFooterView deprecatedFooterView8 = new PaywallFooterView(context, offering, listener, fontProvider, condensed, () -> null);
        OriginalTemplatePaywallFooterView footerView = new OriginalTemplatePaywallFooterView(context);
        OriginalTemplatePaywallFooterView footerView2 = new OriginalTemplatePaywallFooterView(context, attrs);
        OriginalTemplatePaywallFooterView footerView3 = new OriginalTemplatePaywallFooterView(context, attrs, defStyleAttr);
        OriginalTemplatePaywallFooterView footerView4 = new OriginalTemplatePaywallFooterView(context, offering);
        OriginalTemplatePaywallFooterView footerView5 = new OriginalTemplatePaywallFooterView(context, offering, listener);
        OriginalTemplatePaywallFooterView footerView6 = new OriginalTemplatePaywallFooterView(context, offering, listener, fontProvider);
        OriginalTemplatePaywallFooterView footerView7 = new OriginalTemplatePaywallFooterView(context, offering, listener, fontProvider, condensed);
        OriginalTemplatePaywallFooterView footerView8 = new OriginalTemplatePaywallFooterView(context, offering, listener, fontProvider, condensed, () -> null);
    }

    static void checkDeprecatedMethods(PaywallFooterView footerView, PaywallListener listener) {
        footerView.setPaywallListener(null);
        footerView.setPaywallListener(listener);
        footerView.setDismissHandler(null);
        footerView.setDismissHandler(() -> null);
        footerView.setOfferingId(null);
        footerView.setOfferingId("offeringId");
    }

    static void checkMethods(OriginalTemplatePaywallFooterView footerView, PaywallListener listener) {
        footerView.setPaywallListener(null);
        footerView.setPaywallListener(listener);
        footerView.setDismissHandler(null);
        footerView.setDismissHandler(() -> null);
        footerView.setOfferingId(null);
        footerView.setOfferingId("offeringId");
    }
}
