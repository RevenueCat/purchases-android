package com.revenuecat.apitester.java;

import android.net.Uri;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfos;
import com.revenuecat.purchases.models.Transaction;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
final class CustomerInfoAPI {
    static void check(final CustomerInfo customerInfo) {
        final EntitlementInfos entitlementInfo = customerInfo.getEntitlements();
        final Set<String> asubs = customerInfo.getActiveSubscriptions();
        final Set<String> skus = customerInfo.getAllPurchasedSkus();
        final Date led = customerInfo.getLatestExpirationDate();

        final List<Transaction> nst = customerInfo.getNonSubscriptionTransactions();
        final Date opd = customerInfo.getOriginalPurchaseDate();
        final Date rd = customerInfo.getRequestDate();
        final Date fs = customerInfo.getFirstSeen();
        final String oaui = customerInfo.getOriginalAppUserId();
        final Uri mu = customerInfo.getManagementURL();

        final Date eds = customerInfo.getExpirationDateForSku("");
        final Date pds = customerInfo.getPurchaseDateForSku("");
        final Date ede = customerInfo.getExpirationDateForEntitlement("");
        final Date pde = customerInfo.getPurchaseDateForEntitlement("");
        final Map<String, Date> allExpirationDatesByProduct = customerInfo.getAllExpirationDatesByProduct();
        final Map<String, Date> allPurchaseDatesByProduct = customerInfo.getAllPurchaseDatesByProduct();
    }
}
