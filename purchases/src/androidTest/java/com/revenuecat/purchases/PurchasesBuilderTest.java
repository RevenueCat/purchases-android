package com.revenuecat.purchases;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Handler;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class PurchasesBuilderTest {

    private Purchases.PurchasesListener listener = new Purchases.PurchasesListener() {
        @Override
        public void onCompletedPurchase(PurchaserInfo purchaserInfo) {

        }

        @Override
        public void onFailedPurchase(Exception reason) {

        }

        @Override
        public void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo) {

        }
    };

    @Before
    public void setup() {
        Looper.myLooper();
    }


    @Test
    public void canCreateABuilder() {
        createBuilder();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustHaveContext() {
        String apiKey = "api_key";
        new Purchases.Builder(null, apiKey, listener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustHaveAPIKey() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        new Purchases.Builder(appContext, null, listener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void apiKeyMustBeNonEmpty() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        new Purchases.Builder(appContext, "", listener);
    }

    private Purchases.Builder createBuilder() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        String apiKey = "api_key";
        return new Purchases.Builder(appContext, apiKey, listener);
    }

    private void runWithLooper(Runnable runnable) {
        getInstrumentation().runOnMainSync(runnable);
    }

    @Test
    public void canSetAppUserID() {
        final String appUserID = "app_user_id";
        final Purchases.Builder builder = createBuilder();
        builder.appUserID(appUserID);
        runWithLooper(new Runnable() {
            @Override
            public void run() {
                String apiKey = "api_key";

                Purchases p = builder.build();

                assertEquals(appUserID, p.getAppUserID());

                assertNotNull(p);
            }
        });
    }

    @Test
    public void canBuildAPurchasesObject() {
        runWithLooper(new Runnable() {
            @Override
            public void run() {
                String apiKey = "api_key";

                Purchases.Builder builder = new Purchases.Builder(InstrumentationRegistry.getTargetContext(), apiKey, listener);

                Purchases purchases = builder.build();

                assertNotNull(purchases);
            }
        });
    }
}
