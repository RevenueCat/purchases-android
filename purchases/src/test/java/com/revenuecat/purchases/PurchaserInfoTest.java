package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchaserInfoTest {

    static final String validEmptyPurchaserResponse = "{'subscriber': {'other_purchases': {}, 'subscriptions': {}, 'entitlements': {}}}";
    static final String validFullPurchaserResponse = "{'subscriber': {'other_purchases': {'onetime_purchase': {'purchase_date': '1990-08-30T02:40:36Z'}}, 'subscriptions': {'onemonth_freetrial': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'threemonth_freetrial': {'expires_date': '1990-08-30T02:40:36Z'}}, 'entitlements': { 'pro': {'expires_date': '2100-04-06T20:54:45.975000Z'}, 'old_pro': {'expires_date': '1990-08-30T02:40:36Z'}, 'forever_pro': {'expires_date': null}}}}";
    static final String validTwoProducts = "{'subscriber': {'original_application_version': '1.0','other_purchases': {},'subscriptions':{'product_a': {'expires_date': '2018-05-27T06:24:50Z','period_type': 'normal'},'product_b': {'expires_date': '2018-05-27T05:24:50Z','period_type': 'normal'}}}}";


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

        assertEquals(4110728085975L, latest.getTime());

    }

    @Test
    public void getExpirationDateForSku() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();

        Date oneMonthDate = info.getExpirationDateForSku("onemonth_freetrial");
        Date threeMonthDate = info.getExpirationDateForSku("threemonth_freetrial");

        assertTrue(oneMonthDate.after(threeMonthDate));
    }

    @Test
    public void getExpirationDateTwoProducts() throws JSONException {
        PurchaserInfo info = factory.build(new JSONObject(validTwoProducts));
        assertNotNull(info);
    }

    @Test
    public void getExpirationDateForEntitlement() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();

        Date pro = info.getExpirationDateForEntitlement("pro");
        Date oldPro = info.getExpirationDateForEntitlement("old_pro");

        assertTrue(pro.after(oldPro));
    }

    @Test
    public void getsActiveEntitlements() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();
        Set<String> actives = info.getActiveEntitlements();

        assertEquals(2, actives.size());
        assertTrue(actives.contains("pro"));
        assertFalse(actives.contains("old_pro"));
        assertTrue(actives.contains("forever_pro"));
        assertFalse(actives.contains("random"));
    }

    @Test
    public void getExpirationDateForForeverIsNull() throws JSONException {
        PurchaserInfo info = fullPurchaserInfo();

        Date foreverPro = info.getExpirationDateForEntitlement("forever_pro");

        assertNull(foreverPro);
    }

}
