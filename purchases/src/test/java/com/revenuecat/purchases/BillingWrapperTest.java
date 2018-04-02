package com.revenuecat.purchases;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BillingWrapperTest {
    private BillingWrapper.ClientFactory mockClientFactory;
    private BillingClient mockClient;
    private PurchasesUpdatedListener purchasesUpdatedListener;
    private BillingClientStateListener billingClientStateListener;
    private BillingWrapper wrapper;

    @Before
    public void setup() {
        mockClientFactory = mock(BillingWrapper.ClientFactory.class);
        mockClient = mock(BillingClient.class);

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

        wrapper = new BillingWrapper(mockClientFactory);
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

    private List<SkuDetails> skuDetailsList;
    @Test
    public void defersCallingSkuQueryUntilConnected() {
        List<String> productIDs = new ArrayList<String>();
        productIDs.add("product_a");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                SkuDetailsResponseListener listener = invocation.getArgument(1);
                SkuDetails mockDetails = mock(SkuDetails.class);

                List<SkuDetails> details = new ArrayList<>();
                details.add(mockDetails);

                listener.onSkuDetailsResponse(BillingClient.BillingResponse.OK, details);
                return null;
            }
        }).when(mockClient).querySkuDetailsAsync(any(SkuDetailsParams.class), any(SkuDetailsResponseListener.class));

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
}
