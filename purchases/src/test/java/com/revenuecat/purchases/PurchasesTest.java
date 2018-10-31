package com.revenuecat.purchases;

import android.app.Activity;
import android.app.Application;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.revenuecat.purchases.Purchases.AttributionNetwork.APPSFLYER;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PurchasesTest {

    private Application mockApplication = mock(Application.class);
    private BillingWrapper mockBillingWrapper = mock(BillingWrapper.class);
    private BillingWrapper.Factory mockBillingWrapperFactory = mock(BillingWrapper.Factory.class);
    private Backend  mockBackend = mock(Backend.class);
    private DeviceCache  mockCache = mock(DeviceCache.class);


    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private BillingWrapper.PurchasesUpdatedListener purchasesUpdatedListener;

    private String appUserId = "fakeUserID";

    private Purchases.PurchasesListener listener = mock(Purchases.PurchasesListener.class);

    private Purchases purchases;

    private void setup() {

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                activityLifecycleCallbacks = invocation.getArgument(0);
                return null;
            }
        }).when(mockApplication).registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Backend.BackendResponseHandler handler = invocation.getArgument(1);
                handler.onReceivePurchaserInfo(mock(PurchaserInfo.class));
                return null;
            }
        }).when(mockBackend).getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Backend.BackendResponseHandler handler = invocation.getArgument(4);
                handler.onReceivePurchaserInfo(mock(PurchaserInfo.class));
                return null;
            }
        }).when(mockBackend).postReceiptData(any(String.class),
                any(String.class),
                any(String.class),
                any(Boolean.class),
                any(Backend.BackendResponseHandler.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                purchasesUpdatedListener = invocation.getArgument(0);
                return mockBillingWrapper;
            }
        }).when(mockBillingWrapperFactory).buildWrapper(any(BillingWrapper.PurchasesUpdatedListener.class));

        PurchaserInfo mockInfo = mock(PurchaserInfo.class);
        when(mockCache.getCachedPurchaserInfo(any(String.class))).thenReturn(mockInfo);

        purchases = new Purchases(mockApplication, appUserId, listener, mockBackend, mockBillingWrapperFactory, mockCache);
    }

    @Test
    public void canBeCreated() {
        setup();
        assertNotNull(purchases);
    }

    private void mockSkuDetailFetch(final List<SkuDetails> details, List<String> skus, String skuType) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
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
        setup();

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
        setup();

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
        setup();

        Activity activity = mock(Activity.class);
        String sku = "onemonth_freetrial";
        ArrayList<String> oldSkus = new ArrayList<>();

        purchases.makePurchase(activity, sku, BillingClient.SkuType.SUBS);

        verify(mockBillingWrapper).makePurchaseAsync(activity, appUserId, sku, oldSkus, BillingClient.SkuType.SUBS);
    }

    @Test
    public void postsSuccessfulPurchasesToBackend() {
        setup();

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
                eq(false),
                any(Backend.BackendResponseHandler.class));

        verify(mockBillingWrapper, times(1)).consumePurchase(eq(purchaseToken));
    }

    @Test
    public void callsPostForEachUpdatedPurchase() {
        setup();

        List<Purchase> purchasesList = new ArrayList<>();
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        for (int i = 0; i < 2; i++) {
            Purchase p = mock(Purchase.class);
            when(p.getSku()).thenReturn(sku);
            when(p.getPurchaseToken()).thenReturn(purchaseToken + Integer.toString(i));
            purchasesList.add(p);
        }


        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBackend, times(2)).postReceiptData(any(String.class),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void doesntPostIfNotOK() {
        setup();

        purchases.onPurchasesFailedToUpdate(0, "fail");

        verify(mockBackend, times(0)).postReceiptData(any(String.class),
                any(String.class),
                any(String.class),
                eq(false),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void passesUpErrors() {
        setup();

        purchases.onPurchasesFailedToUpdate(0, "");

        verify(listener).onFailedPurchase(eq(Purchases.ErrorDomains.PLAY_BILLING), eq(0), any(String.class));
    }

    @Test
    public void addsAnApplicationLifecycleListener() {
        setup();

        verify(mockApplication).registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
    }

    @Test
    public void closingUnregistersLifecycleListener() {
        setup();

        purchases.close();

        verify(mockApplication).unregisterActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
    }

    @Test
    public void onResumeGetsSubscriberInfo() {
        setup();

        activityLifecycleCallbacks.onActivityResumed(mock(Activity.class));

        verify(mockBackend).getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler.class));
        verify(listener, times(3)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
    }

    @Test
    public void getsSubscriberInfoOnCreated() {
        setup();

        verify(mockBackend).getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void canBeSetupWithoutAppUserID() {
        setup();

        Purchases purchases = new Purchases(mockApplication, null, listener, mockBackend, mockBillingWrapperFactory, mockCache);
        assertNotNull(purchases);

        String appUserID = purchases.getAppUserID();
        assertNotNull(appUserID);
        assertEquals(36, appUserID.length());
    }

    @Test
    public void storesGeneratedAppUserID() {
        setup();

        new Purchases(mockApplication, null, listener, mockBackend, mockBillingWrapperFactory, mockCache);
        verify(mockCache).cacheAppUserID(any(String.class));
    }

    @Test
    public void pullsUserIDFromCache() {
        setup();

        String appUserID = "random_id";
        when(mockCache.getCachedAppUserID()).thenReturn(appUserID);
        Purchases p = new Purchases(mockApplication, null, listener, mockBackend, mockBillingWrapperFactory, mockCache);
        assertEquals(appUserID, p.getAppUserID());
    }

    @Test
    public void isRestoreWhenUsingNullAppUserID() {
        setup();

        Purchases purchases = new Purchases(mockApplication, null, listener, mockBackend, mockBillingWrapperFactory, mockCache);

        Purchase p = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);

        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBackend).postReceiptData(eq(purchaseToken),
                eq(purchases.getAppUserID()),
                eq(sku),
                eq(true),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void doesntRestoreNormally() {
        setup();

        Purchases purchases = new Purchases(mockApplication, "a_fixed_id", listener, mockBackend, mockBillingWrapperFactory, mockCache);

        Purchase p = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);

        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBackend).postReceiptData(eq(purchaseToken),
                eq(purchases.getAppUserID()),
                eq(sku),
                eq(false),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void canOverideAnonMode() {
        setup();

        Purchases purchases = new Purchases(mockApplication, "a_fixed_id", listener, mockBackend, mockBillingWrapperFactory, mockCache);
        purchases.setIsUsingAnonymousID(true);

        Purchase p = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);

        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBackend).postReceiptData(eq(purchaseToken),
                eq(purchases.getAppUserID()),
                eq(sku),
                eq(true),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void restoringPurchasesGetsHistory() {
        setup();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                historyListener = invocation.getArgument(1);
                historyListener.onReceivePurchaseHistory(new ArrayList<Purchase>());
                return null;
            }
        }).when(mockBillingWrapper).queryPurchaseHistoryAsync(any(String.class),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        purchases.restorePurchasesForPlayStoreAccount();

        verify(mockBillingWrapper, times(2)).queryPurchaseHistoryAsync(eq(BillingClient.SkuType.SUBS),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        verify(mockBillingWrapper).queryPurchaseHistoryAsync(eq(BillingClient.SkuType.INAPP),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));
    }

    private BillingWrapper.PurchaseHistoryResponseListener historyListener;
    @Test
    public void historicalPurchasesPassedToBackend() {
        setup();

        Purchase p = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        final List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                historyListener = invocation.getArgument(1);
                historyListener.onReceivePurchaseHistory(purchasesList);
                return null;
            }
        }).when(mockBillingWrapper).queryPurchaseHistoryAsync(any(String.class),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        purchases.restorePurchasesForPlayStoreAccount();

        verify(mockBackend, times(1)).postReceiptData(eq(purchaseToken),
                eq(purchases.getAppUserID()),
                eq(sku),
                eq(true),
                any(Backend.BackendResponseHandler.class));

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
        verify(listener, times(1)).onRestoreTransactions(any(PurchaserInfo.class));
        verify(listener, times(0)).onCompletedPurchase(any(String.class), any(PurchaserInfo.class));
    }

    @Test
    public void failedToRestorePurchases() {
        setup();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                historyListener = invocation.getArgument(1);
                historyListener.onReceivePurchaseHistoryError(0, "Broken");
                return null;
            }
        }).when(mockBillingWrapper).queryPurchaseHistoryAsync(any(String.class),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        purchases.restorePurchasesForPlayStoreAccount();

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
        verify(listener, times(1)).onRestoreTransactionsFailed(Purchases.ErrorDomains.PLAY_BILLING, 0, "Broken");
        verify(listener, times(0)).onCompletedPurchase(any(String.class), any(PurchaserInfo.class));
    }

    @Test
    public void restoringCallsRestoreCallback() {
        setup();

        Purchase p = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        final List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                BillingWrapper.PurchaseHistoryResponseListener listener = invocation.getArgument(1);
                listener.onReceivePurchaseHistory(purchasesList);
                return null;
            }
        }).when(mockBillingWrapper).queryPurchaseHistoryAsync(eq(BillingClient.SkuType.SUBS),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                BillingWrapper.PurchaseHistoryResponseListener listener = invocation.getArgument(1);
                listener.onReceivePurchaseHistory(new ArrayList<Purchase>());
                return null;
            }
        }).when(mockBillingWrapper).queryPurchaseHistoryAsync(eq(BillingClient.SkuType.INAPP),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Backend.BackendResponseHandler handler = invocation.getArgument(4);
                handler.onReceivePurchaserInfo(mock(PurchaserInfo.class));
                return null;
            }
        }).when(mockBackend).postReceiptData(any(String.class),
                any(String.class),
                any(String.class),
                eq(true),
                any(Backend.BackendResponseHandler.class));

        purchases.restorePurchasesForPlayStoreAccount();

        verify(mockBillingWrapper, times(3)).queryPurchaseHistoryAsync(
                any(String.class),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));
        verify(listener, times(1)).onRestoreTransactions(any(PurchaserInfo.class));
    }

    @Test
    public void doesntDoublePostReceipts() {
        setup();

        Purchase p1 = mock(Purchase.class);
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        when(p1.getSku()).thenReturn(sku);
        when(p1.getPurchaseToken()).thenReturn(purchaseToken);

        Purchase p2 = mock(Purchase.class);

        when(p2.getSku()).thenReturn(sku);
        when(p2.getPurchaseToken()).thenReturn(purchaseToken);

        Purchase p3 = mock(Purchase.class);
        when(p3.getSku()).thenReturn(sku);
        when(p3.getPurchaseToken()).thenReturn(purchaseToken + "diff");

        final List<Purchase> purchasesList = new ArrayList<>();
        purchasesList.add(p1);
        purchasesList.add(p2);
        purchasesList.add(p3);

        purchasesUpdatedListener.onPurchasesUpdated(purchasesList);

        verify(mockBackend, times(2)).postReceiptData(any(String.class),
                eq(purchases.getAppUserID()),
                eq(sku),
                eq(false),
                any(Backend.BackendResponseHandler.class));
    }

    @Test
    public void cachedUserInfoShouldGoToListener() {
        setup();

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
    }

    @Test
    public void cachedUserInfoEmitOnResumeActive() {
        setup();

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
        purchases.onActivityResumed(mock(Activity.class));
        verify(listener, times(3)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
    }

    @Test
    public void receivedPurchaserInfoShouldBeCached() {
        setup();

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
                eq(false),
                any(Backend.BackendResponseHandler.class));

        verify(mockCache, times(2)).cachePurchaserInfo(any(String.class), any(PurchaserInfo.class));
    }

    @Test
    public void getEntitlementsHitsBackend() {
        mockProducts(new ArrayList<String>());
        mockSkuDetails(new ArrayList<String>(), new ArrayList<String>(), "subs");

        setup();

        verify(mockBackend).getEntitlements(any(String.class), any(Backend.EntitlementsResponseHandler.class));
    }

    private Map<String, Entitlement> receivedEntitlementMap = null;

    private void mockProducts(final List<String> skus) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Backend.EntitlementsResponseHandler handler = invocation.getArgument(1);
                Map<String, Offering> offeringMap = new HashMap<>();

                for (String sku : skus) {
                    Offering o = new Offering(sku);
                    offeringMap.put(sku + "_offering", o);
                }

                Map<String, Entitlement> entitlementMap = new HashMap<>();
                Entitlement e = new Entitlement(offeringMap);
                entitlementMap.put("pro", e);

                handler.onReceiveEntitlements(entitlementMap);
                return null;
            };
        }).when(mockBackend).getEntitlements(any(String.class), any(Backend.EntitlementsResponseHandler.class));
    }

    private List<SkuDetails> mockSkuDetails(List<String> skus, List<String> returnSkus, String type) {
        List<SkuDetails> skuDetails = new ArrayList<>();

        for (String sku : returnSkus) {
            SkuDetails details = mock(SkuDetails.class);
            when(details.getSku()).thenReturn(sku);
            skuDetails.add(details);
        }

        mockSkuDetailFetch(skuDetails, skus, type);
        return skuDetails;
    }

    @Test
    public void getEntitlementsPopulatesMissingSkuDetails() {
        List<String> skus = new ArrayList<>();
        skus.add("monthly");

        mockProducts(skus);
        List<SkuDetails> details = mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS);

        setup();

        purchases.getEntitlements(new Purchases.GetEntitlementsHandler() {
            @Override
            public void onReceiveEntitlements(Map<String, Entitlement> entitlementMap) {
                PurchasesTest.this.receivedEntitlementMap = entitlementMap;
            }

            @Override
            public void onReceiveEntitlementsError(int domain, int code, String message) {

            }
        });

        assertNotNull(receivedEntitlementMap);

        verify(mockBillingWrapper, times(1)).querySkuDetailsAsync(eq(BillingClient.SkuType.SUBS), eq(skus), any(BillingWrapper.SkuDetailsResponseListener.class));

        Entitlement e = receivedEntitlementMap.get("pro");
        assertEquals(1, e.getOfferings().size());
        Offering o = e.getOfferings().get("monthly_offering");
        assertSame(details.get(0), o.getSkuDetails());
    }

    @Test
    public void getEntitlementsDoesntCheckInappsUnlessThereAreMissingSubs() {

        List<String> skus = new ArrayList<>();
        List<String> subsSkus = new ArrayList<>();
        skus.add("monthly");
        subsSkus.add("monthly");

        List<String> inappSkus = new ArrayList<>();
        skus.add("monthly_inapp");
        inappSkus.add("monthly_inapp");

        mockProducts(skus);
        mockSkuDetails(skus, subsSkus, BillingClient.SkuType.SUBS);
        mockSkuDetails(inappSkus, inappSkus, BillingClient.SkuType.INAPP);

        setup();

        verify(mockBillingWrapper).querySkuDetailsAsync(eq(BillingClient.SkuType.SUBS), eq(skus), any(BillingWrapper.SkuDetailsResponseListener.class));
        verify(mockBillingWrapper).querySkuDetailsAsync(eq(BillingClient.SkuType.INAPP), eq(inappSkus), any(BillingWrapper.SkuDetailsResponseListener.class));
    }

    @Test
    public void getEntitlementsIsCached() {

        List<String> skus = new ArrayList<>();
        skus.add("monthly");
        mockProducts(skus);

        mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS);

        setup();

        verify(mockBackend, times(1)).getEntitlements(eq(appUserId), any(Backend.EntitlementsResponseHandler.class));

        purchases.getEntitlements(new Purchases.GetEntitlementsHandler() {
            @Override
            public void onReceiveEntitlements(Map<String, Entitlement> entitlementMap) {
                PurchasesTest.this.receivedEntitlementMap = entitlementMap;
            }

            @Override
            public void onReceiveEntitlementsError(int domain, int code, String message) {

            }
        });

        assertNotNull(receivedEntitlementMap);
    }

    @Test
    public void getEntitlementsErrorIsCalledIfSkuDetailsMissing() {

        setup();

        List<String> skus = new ArrayList<>();
        skus.add("monthly");
        mockProducts(skus);
        mockSkuDetails(skus, new ArrayList<String>(), BillingClient.SkuType.SUBS);
        mockSkuDetails(skus, new ArrayList<String>(), BillingClient.SkuType.INAPP);

        final String[] errorMessage = {null};


        purchases.getEntitlements(new Purchases.GetEntitlementsHandler() {
            @Override
            public void onReceiveEntitlements(Map<String, Entitlement> entitlementMap) {
                PurchasesTest.this.receivedEntitlementMap = entitlementMap;
            }

            @Override
            public void onReceiveEntitlementsError(int domain, int code, String message) {
                errorMessage[0] = message;
            }
        });

        assertNull(errorMessage[0]);
        assertNotNull(this.receivedEntitlementMap);
    }

    @Test
    public void getEntitlementsErrorIsCalledIfNoBackendResponse() {

        setup();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Backend.EntitlementsResponseHandler handler = invocation.getArgument(1);
                handler.onError(0, "nope");
                return null;
            }
        }).when(mockBackend).getEntitlements(any(String.class), any(Backend.EntitlementsResponseHandler.class));

        final String[] errorMessage = {null};

        purchases.getEntitlements(new Purchases.GetEntitlementsHandler() {
            @Override
            public void onReceiveEntitlements(Map<String, Entitlement> entitlementMap) {
            }

            @Override
            public void onReceiveEntitlementsError(int domain, int code, String message) {
                errorMessage[0] = message;
            }
        });

        assertNotNull(errorMessage[0]);
    }

    @Test
    public void addAttributionPassesDataToBackend() {
        setup();

        JSONObject object = mock(JSONObject.class);
        @Purchases.AttributionNetwork int network = APPSFLYER;
        purchases.addAttributionData(object, network);

        verify(mockBackend).postAttributionData(appUserId, network, object);
    }

    @Test
    public void addAttributionConvertsStringStringMapToJsonObject() {
        setup();

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        @Purchases.AttributionNetwork int network = APPSFLYER;
        purchases.addAttributionData(map, network);

        verify(mockBackend).postAttributionData(eq(appUserId), eq(network), any(JSONObject.class));
    }

    @Test
    public void consumesNonSubscriptionPurchasesOn40x() {
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        final int code = 402;

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Backend.BackendResponseHandler handler = invocation.getArgument(4);
                handler.onError(code, "This is fake");
                return null;
            }
        }).when(mockBackend).postReceiptData(eq(purchaseToken),
                                             eq(appUserId),
                                             eq(sku),
                                             eq(false),
                                             any(Backend.BackendResponseHandler.class));

        setup();

        Purchase p = mock(Purchase.class);

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);
        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBillingWrapper).consumePurchase(eq(purchaseToken));
    }

    @Test
    public void triesToConsumeNonSubscriptionPurchasesOn50x() {
        String sku = "onemonth_freetrial";
        String purchaseToken = "crazy_purchase_token";

        final int code = 502;

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Backend.BackendResponseHandler handler = invocation.getArgument(4);
                handler.onError(code, "This is fake");
                return null;
            }
        }).when(mockBackend).postReceiptData(eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(Backend.BackendResponseHandler.class));

        setup();

        Purchase p = mock(Purchase.class);

        when(p.getSku()).thenReturn(sku);
        when(p.getPurchaseToken()).thenReturn(purchaseToken);

        List<Purchase> purchasesList = new ArrayList<>();

        purchasesList.add(p);
        purchases.onPurchasesUpdated(purchasesList);

        verify(mockBillingWrapper).consumePurchase(eq(purchaseToken));
    }

    @Test
    public void closeCloses() {
        setup();
        purchases.close();

        verify(mockBackend).close();
        verify(mockBillingWrapper).close();
    }

    @Test
    public void whenNoTokensRestoringPurchasesStillCallListener() {
        setup();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                historyListener = invocation.getArgument(1);
                historyListener.onReceivePurchaseHistory(new ArrayList<Purchase>());
                return null;
            }
        }).when(mockBillingWrapper).queryPurchaseHistoryAsync(any(String.class),
                any(BillingWrapper.PurchaseHistoryResponseListener.class));

        purchases.restorePurchasesForPlayStoreAccount();

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo.class));
        verify(listener).onRestoreTransactions(any(PurchaserInfo.class));
    }
}
