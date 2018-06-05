package com.revenuecat.purchases;

import com.revenuecat.purchases.util.Iso8601Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PurchaserInfo {

    static class Factory {
        private Map<String, Date> parseExpirations(JSONObject expirations) throws JSONException {

            Map<String, Date> expirationDates = new HashMap<>();

            for (Iterator<String> it = expirations.keys(); it.hasNext();) {
                String key = it.next();
                String dateValue = expirations.getJSONObject(key).getString("expires_date");

                try {
                    Date date = Iso8601Utils.parse(dateValue);
                    expirationDates.put(key, date);
                } catch (RuntimeException e) {
                    throw new JSONException(e.getMessage());
                }
            }

            return expirationDates;
        }

        PurchaserInfo build(JSONObject object) throws JSONException {
            JSONObject subscriber = object.getJSONObject("subscriber");

            JSONObject otherPurchases = subscriber.getJSONObject("other_purchases");
            Set<String> nonSubscriptionPurchases = new HashSet<>();

            for (Iterator<String> it = otherPurchases.keys(); it.hasNext(); ) {
                String key = it.next();
                nonSubscriptionPurchases.add(key);
            }

            JSONObject subscriptions = subscriber.getJSONObject("subscriptions");
            Map<String, Date> expirationDatesByProduct = parseExpirations(subscriptions);

            JSONObject entitlements = subscriber.getJSONObject("entitlements");
            Map<String, Date> expirationDatesByEntitlement = parseExpirations(entitlements);

            return new PurchaserInfo(nonSubscriptionPurchases, expirationDatesByProduct, expirationDatesByEntitlement, object);
        }
    }

    private final Set<String> nonSubscriptionPurchases;
    private final Map<String, Date> expirationDatesByProduct;
    private final Map<String, Date> expirationDatesByEntitlement;
    private final JSONObject originalJSON;

    private PurchaserInfo(Set<String> nonSubscriptionPurchases,
                          Map<String, Date> expirationDatesByProduct,
                          Map<String, Date> expirationDatesByEntitlement,
                          JSONObject originalJSON) {
        this.nonSubscriptionPurchases = nonSubscriptionPurchases;
        this.expirationDatesByProduct = expirationDatesByProduct;
        this.expirationDatesByEntitlement = expirationDatesByEntitlement;
        this.originalJSON = originalJSON;
    }

    JSONObject getJSONObject() {
        return originalJSON;
    }

    private Set<String> activeIdentifiers(Map<String, Date> expirations) {
        Set<String> activeSkus = new HashSet<>();

        for (String key : expirations.keySet()) {
            Date date = expirations.get(key);
            if (date.after(new Date())) {
                activeSkus.add(key);
            }
        }

        return activeSkus;
    }

    /**
     * @return Set of active subscription skus
     */
    public Set<String> getActiveSubscriptions() {
        return activeIdentifiers(expirationDatesByProduct);
    }

    /**
     * @return Set of purchased skus, active and inactive
     */
    public Set<String> getAllPurchasedSkus() {
        Set<String> appSKUs = new HashSet<>(this.getPurchasedNonSubscriptionSkus());
        appSKUs.addAll(expirationDatesByProduct.keySet());
        return appSKUs;
    }

    /**
     * @return Set of non-subscription, non-consumed skus
     */
    public Set<String> getPurchasedNonSubscriptionSkus() {
        return this.nonSubscriptionPurchases;
    }

    /**
     * @return The latest expiration date of all purchased skus
     */
    public Date getLatestExpirationDate() {
        Date latest = null;

        for (Date date : expirationDatesByProduct.values()) {
            if (latest == null || date.after(latest)) {
                latest = date;
            }
        }

        return latest;
    }

    /**
     * @param sku
     * @return Expiration date for given sku
     */
    public Date getExpirationDateForSku(final String sku) {
        return expirationDatesByProduct.get(sku);
    }

    public Date getExpirationDateForEntitlement(String entitlement) {
        return expirationDatesByEntitlement.get(entitlement);
    }

    public Set<String> getActiveEntitlements() {
        return activeIdentifiers(expirationDatesByEntitlement);
    }

    /**
     * @return Map of skus to dates
     */
    public Map<String, Date> getAllExpirationDatesByProduct() { return expirationDatesByProduct; };
    public Map<String, Date> getAllExpirationDatesByEntitlement() { return expirationDatesByEntitlement; };
}
