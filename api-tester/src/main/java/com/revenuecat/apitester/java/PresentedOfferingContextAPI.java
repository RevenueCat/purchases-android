package com.revenuecat.apitester.java;

import com.revenuecat.purchases.PresentedOfferingContext;

@SuppressWarnings({"unused"})
final class PresentedOfferingContextAPI {
    static void check(final PresentedOfferingContext presentedOfferingContext) {
        final String offeringIdentifier = presentedOfferingContext.getOfferingIdentifier();
        final String placementIdentifier = presentedOfferingContext.getPlacementIdentifier();
        final PresentedOfferingContext.TargetingContext targetingContext = presentedOfferingContext.getTargetingContext();
    }

    static void checkConstructor(final String offeringIdentifier) {
        final PresentedOfferingContext presentedOfferingContext = new PresentedOfferingContext(offeringIdentifier, null, null);
    }
}

final class TargetingContextAPI {
    static void check(final PresentedOfferingContext.TargetingContext targetingContext) {
        final int revision = targetingContext.getRevision();
        final String ruleId = targetingContext.getRuleId();
    }

    static void checkConstructor(final int revision, final String ruleId) {
        final PresentedOfferingContext.TargetingContext targetingContext = new PresentedOfferingContext.TargetingContext(revision, ruleId);
    }
}