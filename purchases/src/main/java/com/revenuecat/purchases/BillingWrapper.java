package com.revenuecat.purchases;

import android.content.Context;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private boolean clientConnected;
    private Queue<Runnable> serviceRequests = new ConcurrentLinkedQueue<>();

    BillingWrapper(ClientFactory clientFactory) {
        billingClient = clientFactory.buildClient(this);

        billingClient.startConnection(this);
    }

    private void executePendingRequests() {
        while (clientConnected && !serviceRequests.isEmpty()) {
            Runnable request = serviceRequests.remove();
            request.run();
        }
    }

    private void executeRequest(final Runnable request) {
        serviceRequests.add(request);
        if (!clientConnected) {
            billingClient.startConnection(this);
        } else {
            executePendingRequests();
        }
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType,
                                     final List<String> skuList,
                                     final SkuDetailsResponseListener listener) {
        executeRequest(new Runnable() {
            @Override
            public void run() {
                SkuDetailsParams params = SkuDetailsParams.newBuilder()
                        .setType(itemType).setSkusList(skuList).build();
                billingClient.querySkuDetailsAsync(params, new com.android.billingclient.api.SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
                        listener.onReceiveSkuDetails(skuDetailsList);
                    }
                });
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }

    @Override
    public void onBillingSetupFinished(int responseCode) {
        if (responseCode == BillingClient.BillingResponse.OK) {
            clientConnected = true;
            executePendingRequests();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        clientConnected = false;
    }
}
