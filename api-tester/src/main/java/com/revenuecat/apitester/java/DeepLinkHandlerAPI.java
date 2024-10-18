package com.revenuecat.apitester.java;

import android.content.Intent;

import com.revenuecat.purchases.deeplinks.DeepLinkHandler;

@SuppressWarnings({"unused"})
final class DeepLinkHandlerAPI {
    static void check(Intent intent, Boolean shouldCache) {
        DeepLinkHandler.HandleResult result = DeepLinkHandler.Companion.handleDeepLink(intent, shouldCache);
    }

    static boolean checkResult(DeepLinkHandler.HandleResult handleResult) {
        switch (handleResult) {
            case HANDLED:
            case IGNORED:
            case DEFERRED_TO_SDK_CONFIGURATION:
                return true;
            default:
                return false;
        }
    }
}
