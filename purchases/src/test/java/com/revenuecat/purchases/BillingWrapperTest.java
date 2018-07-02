package com.revenuecat.purchases;

import android.app.Activity;
import android.os.Handler;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BillingWrapperTest {
    private BillingWrapper.ClientFactory mockClientFactory;
    private BillingClient mockClient;
    private PurchasesUpdatedListener purchasesUpdatedListener;
    private BillingClientStateListener billingClientStateListener;
    private PurchaseHistoryResponseListener billingClientPurchaseHistoryListener;
    private Handler handler;

    private BillingWrapper.PurchasesUpdatedListener mockPurchasesListener;
    private BillingWrapper.PurchaseHistoryResponseListener mockPurchaseHistoryListener;

    private BillingWrapper wrapper;

    private List<SkuDetails> mockDetailsList = new ArrayList<>();

    @Before
    public void setup() {
        mockClientFactory = mock(BillingWrapper.ClientFactory.class);
        mockClient = mock(BillingClient.class);
        mockPurchasesListener = mock(BillingWrapper.PurchasesUpdatedListener.class);
        mockPurchaseHistoryListener = mock(BillingWrapper.PurchaseHistoryResponseListener.class);

        handler = mock(Handler.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = invocation.getArgument(0);
                r.run();
                return null;
            }
        }).when(handler).post(any(Runnable.class));

        when(mockClientFactory.buildClient(any(PurchasesUpdatedListener.class))).thenAnswer(new Answer<BillingClient>() {
            @Override
            public BillingClient answer(InvocationOnMock invocation) throws Throwable {
                purchasesUpdatedListener = invocation.getArgument(0);
                return mockClient;
            }
        });

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                billingClientStateListener = invocation.getArgument(0);
                return null;
            }
        }).when(mockClient).startConnection(any(BillingClientStateListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                billingClientPurchaseHistoryListener = invocation.getArgument(1);
                return null;
            }
        }).when(mockClient).queryPurchaseHistoryAsync(any(String.class), any(PurchaseHistoryResponseListener.class));

        SkuDetails mockDetails = mock(SkuDetails.class);
        mockDetailsList.add(mockDetails);

        wrapper = new BillingWrapper(mockClientFactory, mockPurchasesListener, handler);
    }

    @Test
    public void canBeCreated() {
        assertNotNull(wrapper);
    }

    @Test
    public void callsBuildOnTheFactory() {
        verify(mockClientFactory).buildClient(purchasesUpdatedListener);
    }

    @Test
    public void connectsToPlayBilling() {
        verify(mockClient).startConnection(billingClientStateListener);
    }

    private void mockStandardSkuDetailsResponse() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SkuDetailsResponseListener listener = invocation.getArgument(1);

                listener.onSkuDetailsResponse(BillingClient.BillingResponse.OK, mockDetailsList);
                return null;
            }
        }).when(mockClient).querySkuDetailsAsync(any(SkuDetailsParams.class), any(SkuDetailsResponseListener.class));
    }

    private List<SkuDetails> skuDetailsList;
    @Test
    public void defersCallingSkuQueryUntilConnected() {

        mockStandardSkuDetailsResponse();

        List<String> productIDs = new ArrayList<String>();
        productIDs.add("product_a");

        wrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, new BillingWrapper.SkuDetailsResponseListener() {
            @Override
            public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                BillingWrapperTest.this.skuDetailsList = skuDetails;
            }
        });

        assertNull(skuDetailsList);

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);

        assertNotNull(skuDetailsList);
    }

    private int skuDetailsResponseCalled = 0;
    @Test
    public void canDeferMultipleCalls() {
        mockStandardSkuDetailsResponse();

        List<String> productIDs = new ArrayList<String>();
        productIDs.add("product_a");
        BillingWrapper.SkuDetailsResponseListener listener = new BillingWrapper.SkuDetailsResponseListener() {
            @Override
            public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                BillingWrapperTest.this.skuDetailsResponseCalled+= 1;
            }
        };

        wrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, listener);
        wrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, listener);

        assertEquals(0, skuDetailsResponseCalled);

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);

        assertEquals(2, skuDetailsResponseCalled);
    }

    @Test
    public void makingARequestTriggersAConnectionAttempt() {
        mockStandardSkuDetailsResponse();

        List<String> productIDs = new ArrayList<String>();
        productIDs.add("product_a");

        wrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, new BillingWrapper.SkuDetailsResponseListener() {
            @Override
            public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                // DO NOTHING
            }
        });

        verify(mockClient, times(2)).startConnection(billingClientStateListener);
    }

    @Test
    public void canMakeAPurchase() {


        String sku = "product_a";

        ArrayList<String> oldSkus = new ArrayList<String>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);
        wrapper.makePurchaseAsync(activity, "jerry", sku, oldSkus, BillingClient.SkuType.SUBS);

        verify(mockClient).launchBillingFlow(eq(activity), any(BillingFlowParams.class));
    }

    @Test
    public void properlySetsBillingFlowParams() {
        final String appUserID = "jerry";
        final String sku = "product_a";
        final @BillingClient.SkuType String skuType = BillingClient.SkuType.SUBS;

        final ArrayList<String> oldSkus = new ArrayList<String>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                BillingFlowParams params = invocation.getArgument(1);
                assertEquals(sku, params.getSku());
                assertEquals(skuType, params.getSkuType());
                assertEquals(oldSkus, params.getOldSkus());
                assertEquals(appUserID, params.getAccountId());
                return null;
        }
        }).when(mockClient).launchBillingFlow(eq(activity), any(BillingFlowParams.class));

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);
        wrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);
    }

    @Test
    public void defersBillingFlowIfNotConnected() {
        final String appUserID = "jerry";
        final String sku = "product_a";
        final @BillingClient.SkuType String skuType = BillingClient.SkuType.SUBS;

        final ArrayList<String> oldSkus = new ArrayList<String>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        wrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);

        verify(mockClient, times(0)).launchBillingFlow(eq(activity), any(BillingFlowParams.class));
    }

    @Test
    public void callsLaunchFlowFromMainThread() {
        final String appUserID = "jerry";
        final String sku = "product_a";
        final @BillingClient.SkuType String skuType = BillingClient.SkuType.SUBS;

        final ArrayList<String> oldSkus = new ArrayList<String>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        wrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);

        verify(handler, times(0)).post(any(Runnable.class));

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);

        verify(handler).post(any(Runnable.class));
    }

    @Test
    public void purchasesUpdatedCallsAreForwarded() {
        List<Purchase> purchases = new ArrayList<>();

        purchasesUpdatedListener.onPurchasesUpdated(BillingClient.BillingResponse.OK, purchases);

        verify(mockPurchasesListener).onPurchasesUpdated(purchases);
    }

    @Test
    public void purchaseUpdateFailedCalledIfNotOK() {
        purchasesUpdatedListener.onPurchasesUpdated(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED, null);

        verify(mockPurchasesListener, times(0)).onPurchasesUpdated((List<Purchase>) any());
        verify(mockPurchasesListener).onPurchasesFailedToUpdate(anyInt(), any(String.class));
    }

    @Test
    public void queryHistoryCallsListenerIfOk() {
        wrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, mockPurchaseHistoryListener);
        billingClientPurchaseHistoryListener.onPurchaseHistoryResponse(BillingClient.BillingResponse.OK,
                new ArrayList<Purchase>());

        verify(mockPurchaseHistoryListener).onReceivePurchaseHistory((List<Purchase>) any());
    }

    @Test
    public void queryHistoryNotCalledIfNotOK() {
        wrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, mockPurchaseHistoryListener);
        billingClientPurchaseHistoryListener.onPurchaseHistoryResponse(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
                new ArrayList<Purchase>());

        verify(mockPurchaseHistoryListener, times(0)).onReceivePurchaseHistory((List<Purchase>) any());
        verify(mockPurchaseHistoryListener, times(1)).onReceivePurchaseHistoryError(eq(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED), any(String.class));
    }

}
