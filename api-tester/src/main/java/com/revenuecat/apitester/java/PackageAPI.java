package com.revenuecat.apitester.java;

import androidx.annotation.Nullable;

import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PackageType;
import com.revenuecat.purchases.PresentedOfferingContext;
import com.revenuecat.purchases.models.StoreProduct;

import java.net.URL;

@SuppressWarnings({"unused"})
final class PackageAPI {
    static void check(final Package p) {
        final String identifier = p.getIdentifier();
        final PackageType packageType = p.getPackageType();
        final StoreProduct product = p.getProduct();
        final String offering = p.getOffering();
        final PresentedOfferingContext offeringContext = p.getPresentedOfferingContext();
        final @Nullable URL webCheckoutURL = p.getWebCheckoutURL();
    }

    static void check(final PackageType type) {
        switch (type) {
            case UNKNOWN:
            case CUSTOM:
            case LIFETIME:
            case ANNUAL:
            case SIX_MONTH:
            case THREE_MONTH:
            case TWO_MONTH:
            case MONTHLY:
            case WEEKLY:
        }
    }
}
