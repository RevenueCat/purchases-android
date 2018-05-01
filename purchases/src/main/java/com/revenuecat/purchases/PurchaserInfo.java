package com.revenuecat.purchases;

import com.revenuecat.purchases.util.Iso8601Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PurchaserInfo {

    static class Factory {
        PurchaserInfo build(JSONObject object) throws JSONException {
            JSONObject subscriber = object.getJSONObject("subscriber");

            JSONObject otherPurchases = subscriber.getJSONObject("other_purchases");
            Set<String> nonSubscriptionPurchases = new HashSet<>();

            for (Iterator<String> it = otherPurchases.keys(); it.hasNext(); ) {
                String key = it.next();
                nonSubscriptionPurchases.add(key);
            }

            JSONObject subscriptions = subscriber.getJSONObject("subscriptions");
            Map<String, Date> expirationDates = new HashMap<>();

            for (Iterator<String> it = subscriptions.keys(); it.hasNext();) {
               String key = it.next();
               String dateValue = subscriptions.getJSONObject(key).getString("expires_date");

               try {
                   Date date = Iso8601Utils.parse(dateValue);
                   expirationDates.put(key, date);
               } catch (RuntimeException e) {
                   throw new JSONException(e.getMessage());
               }
            }

            return new PurchaserInfo(nonSubscriptionPurchases, expirationDates, object);
        }
    }

    private final Set<String> nonSubscriptionPurchases;
    private final Map<String, Date> expirationDates;
    private final JSONObject originalJSON;

    private PurchaserInfo(Set<String> nonSubscriptionPurchases, Map<String, Date> expirationDates, JSONObject originalJSON) {
        this.nonSubscriptionPurchases = nonSubscriptionPurchases;
        this.expirationDates = expirationDates;
        this.originalJSON = originalJSON;
    }

    JSONObject getJSONObject() {
        return originalJSON;
    }

    /**
     * @return Set of active subscription skus
     */
    public Set<String> getActiveSubscriptions() {
        Set<String> activeSkus = new HashSet<>();

        for (String key : expirationDates.keySet()) {
            Date date = expirationDates.get(key);
            if (date.after(new Date())) {
                activeSkus.add(key);
            }
        }

        return activeSkus;
    }

    /**
     * @return Set of purchased skus, active and inactive
     */
    public Set<String> getAllPurchasedSkus() {
        Set<String> appSKUs = new HashSet<>(this.getPurchasedNonSubscriptionSkus());
        appSKUs.addAll(expirationDates.keySet());
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

        for (Date date : expirationDates.values()) {
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
        return expirationDates.get(sku);
    }

    /**
     * @return Map of skus to dates
     */
    public Map<String, Date> getAllExpirationDates() { return expirationDates; };
}
