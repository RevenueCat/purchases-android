package com.revenuecat.purchases;

import android.content.Context;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

public class BillingWrapper implements PurchasesUpdatedListener, BillingClientStateListener {

    public static class ClientFactory {
        final private Context context;

        public ClientFactory(Context context) {
            this.context = context;
        }

        public BillingClient buildClient(PurchasesUpdatedListener listener) {
            return BillingClient.newBuilder(context).setListener(listener).build();
        }
    }

    public interface SkuDetailsResponseListener {
        void onReceiveSkuDetails(List<SkuDetails> skuDetails);
    }

    final private BillingClient billingClient;

    BillingWrapper(ClientFactory clientFactory) {
        billingClient = clientFactory.buildClient(this);

        billingClient.startConnection(this);
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType,
                                     final List<String> skuList,
                                     final SkuDetailsResponseListener listener) {

    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }

    @Override
    public void onBillingSetupFinished(int responseCode) {

    }

    @Override
    public void onBillingServiceDisconnected() {

    }
}
