package com.revenuecat.purchases;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    private void setup() {
        mockClientFactory = mock(BillingWrapper.ClientFactory.class);
        mockClient = mock(BillingClient.class);
        mockPurchasesListener = mock(BillingWrapper.PurchasesUpdatedListener.class);
        mockPurchaseHistoryListener = mock(BillingWrapper.PurchaseHistoryResponseListener.class);

        handler = mock(Handler.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Runnable r = invocation.getArgument(0);
                r.run();
                return null;
            }
        }).when(handler).post(any(Runnable.class));

        when(mockClientFactory.buildClient(any(PurchasesUpdatedListener.class))).thenAnswer(new Answer<BillingClient>() {
            @Override
            public BillingClient answer(InvocationOnMock invocation) {
                purchasesUpdatedListener = invocation.getArgument(0);
                return mockClient;
            }
        });

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                billingClientStateListener = invocation.getArgument(0);
                return null;
            }
        }).when(mockClient).startConnection(any(BillingClientStateListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                billingClientPurchaseHistoryListener = invocation.getArgument(1);
                return null;
            }
        }).when(mockClient).queryPurchaseHistoryAsync(any(String.class), any(PurchaseHistoryResponseListener.class));

        SkuDetails mockDetails = mock(SkuDetails.class);
        mockDetailsList.add(mockDetails);

        wrapper = new BillingWrapper(mockClientFactory, handler);
        wrapper.setListener(mockPurchasesListener);
    }

    @Test
    public void canBeCreated() {
        setup();
        assertNotNull(wrapper);
    }

    @Test
    public void callsBuildOnTheFactory() {
        setup();
        verify(mockClientFactory).buildClient(purchasesUpdatedListener);
    }

    @Test
    public void connectsToPlayBilling() {
        setup();
        verify(mockClient).startConnection(billingClientStateListener);
    }

    private void mockStandardSkuDetailsResponse() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                SkuDetailsResponseListener listener = invocation.getArgument(1);

                listener.onSkuDetailsResponse(BillingClient.BillingResponse.OK, mockDetailsList);
                return null;
            }
        }).when(mockClient).querySkuDetailsAsync(any(SkuDetailsParams.class), any(SkuDetailsResponseListener.class));
    }

    private List<SkuDetails> skuDetailsList;

    @Test
    public void defersCallingSkuQueryUntilConnected() {
        setup();

        mockStandardSkuDetailsResponse();

        List<String> productIDs = new ArrayList<>();
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
        setup();
        mockStandardSkuDetailsResponse();

        List<String> productIDs = new ArrayList<>();
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
        setup();
        mockStandardSkuDetailsResponse();

        List<String> productIDs = new ArrayList<>();
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
        setup();

        String sku = "product_a";

        ArrayList<String> oldSkus = new ArrayList<>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);
        wrapper.makePurchaseAsync(activity, "jerry", sku, oldSkus, BillingClient.SkuType.SUBS);

        verify(mockClient).launchBillingFlow(eq(activity), any(BillingFlowParams.class));
    }

    @Test
    public void properlySetsBillingFlowParams() {
        setup();
        final String appUserID = "jerry";
        final String sku = "product_a";
        final @BillingClient.SkuType String skuType = BillingClient.SkuType.SUBS;

        final ArrayList<String> oldSkus = new ArrayList<>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
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
        setup();
        final String appUserID = "jerry";
        final String sku = "product_a";
        final @BillingClient.SkuType String skuType = BillingClient.SkuType.SUBS;

        final ArrayList<String> oldSkus = new ArrayList<>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        wrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);

        verify(mockClient, times(0)).launchBillingFlow(eq(activity), any(BillingFlowParams.class));
    }

    @Test
    public void callsLaunchFlowFromMainThread() {
        setup();
        final String appUserID = "jerry";
        final String sku = "product_a";
        final @BillingClient.SkuType String skuType = BillingClient.SkuType.SUBS;

        final ArrayList<String> oldSkus = new ArrayList<>();
        oldSkus.add("product_b");

        Activity activity = mock(Activity.class);

        wrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType);

        verify(handler, times(0)).post(any(Runnable.class));

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);

        verify(handler).post(any(Runnable.class));
    }

    @Test
    public void purchasesUpdatedCallsAreForwarded() {
        setup();
        List<Purchase> purchases = new ArrayList<>();

        purchasesUpdatedListener.onPurchasesUpdated(BillingClient.BillingResponse.OK, purchases);

        verify(mockPurchasesListener).onPurchasesUpdated(purchases);
    }

    @Test
    public void purchasesUpdatedCallsAreForwardedWithEmptyIfOkNull() {
        setup();

        purchasesUpdatedListener.onPurchasesUpdated(BillingClient.BillingResponse.OK, null);

        verify(mockPurchasesListener).onPurchasesFailedToUpdate(eq(BillingClient.BillingResponse.ERROR), any(String.class));
    }

    @Test
    public void purchaseUpdateFailedCalledIfNotOK() {
        setup();
        purchasesUpdatedListener.onPurchasesUpdated(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED, null);

        verify(mockPurchasesListener, times(0)).onPurchasesUpdated((List<Purchase>) any());
        verify(mockPurchasesListener).onPurchasesFailedToUpdate(anyInt(), any(String.class));
    }

    @Test
    public void queryHistoryCallsListenerIfOk() {
        setup();
        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);
        wrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, mockPurchaseHistoryListener);
        billingClientPurchaseHistoryListener.onPurchaseHistoryResponse(BillingClient.BillingResponse.OK,
                new ArrayList<Purchase>());

        verify(mockPurchaseHistoryListener).onReceivePurchaseHistory((List<Purchase>) any());
    }

    @Test
    public void queryHistoryNotCalledIfNotOK() {
        setup();
        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);
        wrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, mockPurchaseHistoryListener);
        billingClientPurchaseHistoryListener.onPurchaseHistoryResponse(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
                new ArrayList<Purchase>());

        verify(mockPurchaseHistoryListener, times(0)).onReceivePurchaseHistory((List<Purchase>) any());
        verify(mockPurchaseHistoryListener, times(1)).onReceivePurchaseHistoryError(eq(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED), any(String.class));
    }

    @Test
    public void canConsumeAToken() {
        setup();
        String token = "mockToken";

        billingClientStateListener.onBillingSetupFinished(BillingClient.BillingResponse.OK);
        wrapper.consumePurchase(token);

        verify(mockClient).consumeAsync(eq(token), any(ConsumeResponseListener.class));
    }

    @Test
    public void removingListenerDisconnects() {
        setup();
        wrapper.setListener(null);
        verify(mockClient).endConnection();
        assertThat(wrapper.purchasesUpdatedListener).isNull();
    }

    @Test
    public void whenSettingListenerStartConnection() {
        setup();
        verify(mockClient).startConnection(eq(wrapper));
        assertThat(wrapper.purchasesUpdatedListener).isNotNull();
    }

    @Test
    public void whenExecutingRequestAndThereIsNoListenerDoNotTryToStartConnection() {
        BillingWrapper.ClientFactory clientFactory = mock(BillingWrapper.ClientFactory.class);
        BillingClient billingClient = mock(BillingClient.class);

        when(clientFactory.buildClient(any(PurchasesUpdatedListener.class)))
                .thenReturn(billingClient);

        BillingWrapper billingWrapper = new BillingWrapper(
                clientFactory,
                mock(Handler.class)
        );

        billingWrapper.setListener(null);
        billingWrapper.consumePurchase("token");

        verify(billingClient, never()).startConnection(eq(billingWrapper));
    }

    @Test
    public void whenSkuDetailsIsNullPassAnEmptyListToTheListener() {
        setup();
        mockNullSkuDetailsResponse();

        List<String> productIDs = new ArrayList<>();
        productIDs.add("product_a");

        wrapper.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, new BillingWrapper.SkuDetailsResponseListener() {
            @Override
            public void onReceiveSkuDetails(List<SkuDetails> skuDetails) {
                assertThat(skuDetails).isNotNull();
                assertThat(skuDetails.size()).isEqualTo(0);
            }
        });
    }

    @Test
    public void nullifyBillingClientAfterEndingConnection() {
        setup();
        wrapper.setListener(null);

        assertThat(wrapper.billingClient).isNull();
    }

    @Test
    public void newBillingClientIsCreatedWhenSettingListener() {
        setup();
        wrapper.setListener(mockPurchasesListener);

        assertThat(wrapper.billingClient).isNotNull();
    }

    private void mockNullSkuDetailsResponse() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                SkuDetailsResponseListener listener = invocation.getArgument(1);

                listener.onSkuDetailsResponse(BillingClient.BillingResponse.OK, null);
                return null;
            }
        }).when(mockClient).querySkuDetailsAsync(any(SkuDetailsParams.class), any(SkuDetailsResponseListener.class));
    }
}
