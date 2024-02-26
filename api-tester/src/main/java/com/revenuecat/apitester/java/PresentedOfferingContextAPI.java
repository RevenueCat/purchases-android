package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PresentedOfferingContext;

@SuppressWarnings({"unused"})
final class PresentedOfferingContextAPI {
    static void check(final PresentedOfferingContext presentedOfferingContext) {
        final String offeringIdentifier = presentedOfferingContext.getOfferingIdentifier();
    }

    static void checkConstructor(final String offeringIdentifier,
                                 final String placementIdentifier,
                                 final Integer targetingRevision,
                                 final String targetingRuleId) {
        final PresentedOfferingContext presentedOfferingContext = new PresentedOfferingContext(
                offeringIdentifier
                );
    }
}
