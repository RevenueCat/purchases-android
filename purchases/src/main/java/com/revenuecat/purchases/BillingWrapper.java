package com.revenuecat.purchases;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BillingWrapper implements PurchasesUpdatedListener, BillingClientStateListener {

    static class ClientFactory {
        final private Context context;

        public ClientFactory(Context context) {
            this.context = context;
        }

        public BillingClient buildClient(com.android.billingclient.api.PurchasesUpdatedListener listener) {
            return BillingClient.newBuilder(context).setListener(listener).build();
        }
    }

    static class Factory {
        private final ClientFactory clientFactory;
        private final Handler mainHandler;

        Factory(ClientFactory clientFactory, Handler mainHandler) {

            this.clientFactory = clientFactory;
            this.mainHandler = mainHandler;
        }
        public BillingWrapper buildWrapper(PurchasesUpdatedListener listener) {
            return new BillingWrapper(clientFactory, listener, mainHandler);
        }
    }

    public interface SkuDetailsResponseListener {
        void onReceiveSkuDetails(List<SkuDetails> skuDetails);
    }

    public interface PurchasesUpdatedListener {
        void onPurchasesUpdated(List<Purchase> purchases);
        void onPurchasesFailedToUpdate(String message);
    }

    final private BillingClient billingClient;
    final private PurchasesUpdatedListener purchasesUpdatedListener;
    final private Handler mainHandler;

    private boolean clientConnected;
    private Queue<Runnable> serviceRequests = new ConcurrentLinkedQueue<>();

    BillingWrapper(ClientFactory clientFactory, PurchasesUpdatedListener purchasesUpdatedListener, Handler mainHandler) {
        billingClient = clientFactory.buildClient(this);
        this.purchasesUpdatedListener = purchasesUpdatedListener;
        this.mainHandler = mainHandler;

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

    private void executeRequestOnUIThread(final Runnable request) {
        executeRequest(new Runnable() {
            @Override
            public void run() {
                mainHandler.post(request);
            }
        });
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

    public void makePurchaseAsync(final Activity activity, final String appUserID, final String sku, final ArrayList<String> oldSkus,
                                  final @BillingClient.SkuType String skuType) {

        executeRequestOnUIThread(new Runnable() {
            @Override
            public void run() {
                BillingFlowParams params = BillingFlowParams.newBuilder()
                        .setSku(sku)
                        .setType(skuType)
                        .setOldSkus(oldSkus)
                        .setAccountId(appUserID).build();
                billingClient.launchBillingFlow(activity, params);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            purchasesUpdatedListener.onPurchasesUpdated(purchases);
        } else {
            purchasesUpdatedListener.onPurchasesFailedToUpdate("idk something failed");
        }
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
