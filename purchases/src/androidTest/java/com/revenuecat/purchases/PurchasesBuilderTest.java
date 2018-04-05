package com.revenuecat.purchases;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PurchasesBuilderTest {

    @Test
    public void canCreateABuilder() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        String apiKey = "api_key";
        new Purchases.Builder(appContext, apiKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustHaveContext() {
        String apiKey = "api_key";
        new Purchases.Builder(null, apiKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustHaveAPIKey() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        new Purchases.Builder(appContext, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void apiKeyMustBeNonEmpty() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        new Purchases.Builder(appContext, "");
    }
}
