package com.revenuecat.apitesterjava;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.EntitlementInfos;

import java.util.Map;

@SuppressWarnings({"unused"})
final class EntitlementInfosAPI {
    static void check(final EntitlementInfos infos) {
        final Map<String, EntitlementInfo> active = infos.getActive();
        final Map<String, EntitlementInfo> all = infos.getAll();
        final EntitlementInfo i = infos.get("");
    }
}
