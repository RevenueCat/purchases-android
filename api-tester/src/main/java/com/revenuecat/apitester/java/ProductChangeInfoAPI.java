package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ProductChangeInfo;

@SuppressWarnings({"unused"})
final class ProductChangeInfoAPI {
    static void check(final ProductChangeInfo productChangeInfo) {
        final String oldProductId = productChangeInfo.getOldSku();
        final Integer prorationMode = productChangeInfo.getProrationMode();

        ProductChangeInfo constructedProductChangeInfo = new ProductChangeInfo(
                productChangeInfo.getOldSku(),
                productChangeInfo.getProrationMode()
        );

        ProductChangeInfo constructedProductChangeInfoNullProrationMode =
                new ProductChangeInfo(productChangeInfo.getOldSku(), null);
    }
}