package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EntitlementTest {

    private final Entitlement.Factory factory = new Entitlement.Factory();

    Map<String, Entitlement> buildEntitlementMap(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        return factory.build(object);
    }

    @Test
    public void factoryHandlesEmptyCase() throws JSONException {
        Map<String, Entitlement> entitlementMap = buildEntitlementMap("{}");
        assertEquals(0, entitlementMap.size());
    }

    @Test
    public void factoryHandlesEntitlementNoOfferings() throws JSONException {
        Map<String, Entitlement> entitlementMap = buildEntitlementMap("{'pro': {'offerings': {}}}");
        assertEquals(1, entitlementMap.size());
        Entitlement e = entitlementMap.get("pro");
        Map<String, Offering> offerings = e.getOfferings();
        assertEquals(0, offerings.size());
    }

    @Test
    public void factoryHandlesOneOffering() throws JSONException {
        Map<String, Entitlement> entitlementMap = buildEntitlementMap("{'pro': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial'}}}}");
        Entitlement e = entitlementMap.get("pro");
        Map<String, Offering> offerings = e.getOfferings();
        assertEquals(1, offerings.size());
        Offering o = offerings.get("monthly");
        assertEquals("onemonth_freetrial", o.getActiveProductIdentifier());
    }

    @Test
    public void factoryHandlesMultipleOfferings() throws JSONException {
        Map<String, Entitlement> entitlementMap = buildEntitlementMap("{'pro': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial'},'annual': {'active_product_identifier': 'oneyear_freetrial'}}}}");
        Entitlement e = entitlementMap.get("pro");
        Map<String, Offering> offerings = e.getOfferings();

        Offering monthly = offerings.get("monthly");
        Offering annual = offerings.get("annual");

        assertEquals("onemonth_freetrial", monthly.getActiveProductIdentifier());
        assertEquals("oneyear_freetrial", annual.getActiveProductIdentifier());
    }

    @Test
    public void factoryHandlesMultipleEntitlments() throws JSONException {
        Map<String, Entitlement> entitlementMap = buildEntitlementMap("{'pro': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial'},'annual': {'active_product_identifier': 'oneyear_freetrial'}}}, 'pro2': {'offerings': {'monthly': {'active_product_identifier': 'onemonth_freetrial2'},'annual': {'active_product_identifier': 'oneyear_freetrial2'}}}}");
        assertEquals(2, entitlementMap.size());
    }
}
