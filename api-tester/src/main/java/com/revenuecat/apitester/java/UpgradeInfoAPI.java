package com.revenuecat.apitester.java;

import com.revenuecat.purchases.ProductChangeInfo;
import com.revenuecat.purchases.models.GoogleProrationMode;

@SuppressWarnings({"unused"})
final class UpgradeInfoAPI {
    static void check(final ProductChangeInfo productChangeInfo) {
        final String oldProductId = productChangeInfo.getOldProductId();
        GoogleProrationMode prorationMode = productChangeInfo.getGoogleProrationMode();

        ProductChangeInfo constructedProductChangeInfo = new ProductChangeInfo(
                productChangeInfo.getOldProductId(),
                productChangeInfo.getGoogleProrationMode()
        );

        ProductChangeInfo constructedProductChangeInfoNullProrationMode = new ProductChangeInfo(productChangeInfo.getOldProductId());

        ProductChangeInfo constructedProductChangeInfoProductIdOnly = new ProductChangeInfo(productChangeInfo.getOldProductId());
        ProductChangeInfo constructedProductChangeInfoProductIdAndOldSkuOnly = new ProductChangeInfo(productChangeInfo.getOldProductId());
    }
}