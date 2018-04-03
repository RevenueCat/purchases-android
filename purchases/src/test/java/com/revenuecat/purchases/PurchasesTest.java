package com.revenuecat.purchases;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PurchasesTest {
    private BillingWrapper mockBillingWrapper = mock(BillingWrapper.class);
    private Backend mockBackend = mock(Backend.class);

    private String apiKey = "fakeapikey";
    private String appUserId = "fakeUserID";

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

    private Purchases purchases;
    @Before
    public void setup() {
        purchases = new Purchases(apiKey, appUserId, listener, mockBackend, mockBillingWrapper);
    }

    @Test
    public void canBeCreated() {
        assertNotNull(purchases);
    }

    private void mockSkuDetailFetch(final List<SkuDetails> details, List<String> skus, String skuType) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                BillingWrapper.SkuDetailsResponseListener listener = invocation.getArgument(2);
                listener.onReceiveSkuDetails(details);
                return null;
            }
        }).when(mockBillingWrapper).querySkuDetailsAsync(eq(skuType),
                eq(skus), any(BillingWrapper.SkuDetailsResponseListener.class));
    }

    private List<SkuDetails> receivedSkus;
    @Test
    public void getsSubscriptionSkus() {
        List<String> skus = new ArrayList<>();
        skus.add("onemonth_freetrial");

        final List<SkuDetails> skuDetails = new ArrayList<>();

        mockSkuDetailFetch(skuDetails, skus, BillingClient.SkuType.SUBS);

        purchases.getSubscriptionSkus(skus, new Purchases.GetSkusResponseHandler() {
            @Override
            public void onReceiveSkus(List<SkuDetails> skus) {
                PurchasesTest.this.receivedSkus = skus;
            }
        });

        assertSame(receivedSkus, skuDetails);
    }

    @Test
    public void getsNonSubscriptionSkus() {
        List<String> skus = new ArrayList<>();
        skus.add("normal_purchase");

        final List<SkuDetails> skuDetails = new ArrayList<>();

        mockSkuDetailFetch(skuDetails, skus, BillingClient.SkuType.INAPP);

        purchases.getNonSubscriptionSkus(skus, new Purchases.GetSkusResponseHandler() {
            @Override
            public void onReceiveSkus(List<SkuDetails> skus) {
                PurchasesTest.this.receivedSkus = skus;
            }
        });

        assertSame(receivedSkus, skuDetails);
    }

    @Test
    public void canMakePurchase() {
        Activity activity = mock(Activity.class);
        String sku = "onemonth_freetrial";
        ArrayList<String> oldSkus = new ArrayList<>();

        purchases.makePurchase(activity, sku, BillingClient.SkuType.SUBS);

        verify(mockBillingWrapper).makePurchaseAsync(activity, appUserId, sku, oldSkus, BillingClient.SkuType.SUBS);
    }
}
