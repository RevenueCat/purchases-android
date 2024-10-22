package com.revenuecat.apitester.java;

import android.content.Intent;

import androidx.annotation.OptIn;

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI;
import com.revenuecat.purchases.deeplinks.DeepLinkHandler;

@OptIn(markerClass = ExperimentalPreviewRevenueCatPurchasesAPI.class)
@SuppressWarnings({"unused"})
final class DeepLinkHandlerAPI {
    static void check(Intent intent, Boolean shouldCache) {
        DeepLinkHandler.Result result = DeepLinkHandler.handleDeepLink(intent, shouldCache);
    }

    static boolean checkResult(DeepLinkHandler.Result result) {
        switch (result) {
            case HANDLED:
            case IGNORED:
            case DEFERRED:
                return true;
            default:
                return false;
        }
    }
}
