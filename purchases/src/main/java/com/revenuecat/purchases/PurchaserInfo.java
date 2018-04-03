package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

class PurchaserInfo {

    public static class Factory {
        private TimeZone tz = TimeZone.getTimeZone("UTC");
        private DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

        Factory() {
            dateFormatter.setTimeZone(tz);
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
            Map<String, Date> expirationDates = new HashMap<>();

            for (Iterator<String> it = subscriptions.keys(); it.hasNext();) {
               String key = it.next();
               String dateValue = subscriptions.getJSONObject(key).getString("expires_date");

                try {
                    Date date = dateFormatter.parse(dateValue);
                    expirationDates.put(key, date);
                } catch (ParseException e) {
                    throw new JSONException(e.getMessage());
                }
            }

            return new PurchaserInfo(nonSubscriptionPurchases, expirationDates);
        }
    }

    private final Set<String> nonSubscriptionPurchases;
    private final Map<String, Date> expirationDates;

    private PurchaserInfo(Set<String> nonSubscriptionPurchases, Map<String, Date> expirationDates) {
        this.nonSubscriptionPurchases = nonSubscriptionPurchases;
        this.expirationDates = expirationDates;
    }

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

    public Set<String> getAllPurchasedSkus() {
        Set<String> appSKUs = new HashSet<>(this.getPurchasedNonSubscriptionSkus());
        appSKUs.addAll(expirationDates.keySet());
        return appSKUs;
    }

    public Set<String> getPurchasedNonSubscriptionSkus() {
        return this.nonSubscriptionPurchases;
    }

    public Date getLatestExpirationDate() {
        Date latest = null;

        for (Date date : expirationDates.values()) {
            if (latest == null || date.after(latest)) {
                latest = date;
            }
        }

        return latest;
    }

    public Date getExpirationDateForSku(final String sku) {
        return expirationDates.get(sku);
    }
}
