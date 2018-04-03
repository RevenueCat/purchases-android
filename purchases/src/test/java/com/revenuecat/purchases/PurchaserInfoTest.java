package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchaserInfoTest {

    private final String validEmptyPurchaserResponse = "{'subscriber': {'other_purchases': {}, 'subscriptions': {}}}";

    private final String validFullPurchaserResponse = "{'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-08-30T02:40:36Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}}}";

    // FactoryTests
    private PurchaserInfo.Factory factory = new PurchaserInfo.Factory();

    private PurchaserInfo fullPurchaserInfo() throws JSONException {
        return factory.build(new JSONObject(validFullPurchaserResponse));
    }

    @Test(expected = Exception.class)
    public void failsToBeCreatedWithEmptyJSONObject() throws JSONException {
        JSONObject empty = new JSONObject("{}");
        factory.build(empty);
    }

    @Test
    public void canCreateEmptyPurchaserInfo() throws JSONException {
        JSONObject jsonObject = new JSONObject(validEmptyPurchaserResponse);

        PurchaserInfo info = factory.build(jsonObject);

        assertNotNull(info);
        assertEquals(0, info.getActiveSubscriptions().size());
        assertEquals(0, info.getAllPurchasedSkus().size());
        assertEquals(0, info.getPurchasedNonSubscriptionSkus().size());
        assertNull(info.getLatestExpirationDate());
    }

    @Test
    public void parsesOtherPurchases() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();
        Set<String> nonSubscriptionSKUs = info.getPurchasedNonSubscriptionSkus();

        assertEquals(1, nonSubscriptionSKUs.size());
        assertTrue(nonSubscriptionSKUs.contains("onetime_purchase"));
    }

    @Test
    public void getsActiveSubscriptions() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();
        Set<String> actives = info.getActiveSubscriptions();

        assertEquals(1, actives.size());
        assertTrue(actives.contains("onemonth_freetrial"));
    }

    @Test
    public void getAllPurchasedSKUs() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();
        Set<String> actives = info.getAllPurchasedSkus();

        assertEquals(3, actives.size());
        assertTrue(actives.contains("onemonth_freetrial"));
        assertTrue(actives.contains("onetime_purchase"));
        assertTrue(actives.contains("threemonth_freetrial"));
    }

    @Test
    public void getLatestExpirationDate() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();

        Date latest = info.getLatestExpirationDate();

        assertNotNull(latest);

        DateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertEquals(formatter.format(new Date(200,7, 29, 19, 40, 36)), formatter.format(latest));
    }

}
