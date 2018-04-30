package com.revenuecat.purchases;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PurchasesTest {
    private Application mockApplication = mock(Application.class);
    private BillingWrapper mockBillingWrapper = mock(BillingWrapper.class);
    private BillingWrapper.Factory mockBillingWrapperFactory = mock(BillingWrapper.Factory.class);
    private Backend mockBackend = mock(Backend.class);

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;

    private String apiKey = "fakeapikey";
    private String appUserId = "fakeUserID";

    private Purchases.PurchasesListener listener = mock(Purchases.PurchasesListener.class);

    private Purchases purchases;
    @Before
    public void setup() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                activityLifecycleCallbacks = invocation.getArgument(0);
                return null;
            }
        }).when(mockApplication).registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Backend.BackendResponseHandler handler = invocation.getArgument(1);
                handler.onReceivePurchaserInfo(mock(PurchaserInfo.class));
                return null;
            }
        }).when(mockBackend).getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler.class));

        when(mockBillingWrapperFactory.buildWrapper(any(BillingWrapper.PurchasesUpdatedListener.class)))
                .thenReturn(mockBillingWrapper);

        purchases = new Purchases(mockApplication, apiKey, appUserId, listener, mockBackend, mockBillingWrapperFactory);
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

    @Test
    public void postsSuccessfulPurchasesToBackend() {
        Purchase p = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);

        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBackend).postReceiptData(eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void callsPostForEachUpdatedPurchase() {
        List<Purchase> purchasesList = new ArrayList<>();
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        for (int i = 0; i < 2; i++) {
            Purchase p = mock(Purchase.class);
            when(p.getSku()).thenReturn(sku);
            when(p.getPurchaseToken()).thenReturn(purchaseToken);
            purchasesList.add(p);
        }


        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBackend, times(2)).postReceiptData(eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void doesntPostIfNotOK() {
        purchases.onPurchasesFailedToUpdate(0, "fail");

        verify(mockBackend, times(0)).postReceiptData(any(String.class),
                any(String.class),
                any(String.class),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void passesUpErrors() {
        purchases.onPurchasesFailedToUpdate(0, "");

        verify(listener).onFailedPurchase(any(Exception.class));
    }

    @Test
    public void addsAnApplicationLifecycleListener() {
        verify(mockApplication).registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
    }

    @Test
    public void onResumeGetsSubscriberInfo() {

        activityLifecycleCallbacks.onActivityResumed(mock(Activity.class));

        verify(mockBackend).getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler.class));
        verify(listener).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
    }

    @Test
    public void getsSubscriberInfoOnCreated() {
        verify(mockBackend).getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void canBeSetupWithoutAppUserID() {
        Purchases purchases = new Purchases(mockApplication, apiKey, null, listener, mockBackend, mockBillingWrapperFactory);
        assertNotNull(purchases);

        String appUserID = purchases.getAppUserID();
        assertNotNull(appUserID);
        assertEquals(36, appUserID.length());
    }
}
