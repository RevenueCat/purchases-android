package com.revenuecat.purchases;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
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

    public interface SkuDetailsResponseListener {
        void onReceiveSkuDetails(@NonNull List<SkuDetails> skuDetails);
    }

    public interface PurchaseHistoryResponseListener {
        void onReceivePurchaseHistory(List<Purchase> purchasesList);
        void onReceivePurchaseHistoryError(@BillingClient.BillingResponse int responseCode, String message);
    }

    public interface PurchasesUpdatedListener {
        void onPurchasesUpdated(List<Purchase> purchases);
        void onPurchasesFailedToUpdate(@BillingClient.BillingResponse int responseCode, String message);
    }

    final private BillingClient billingClient;
    @VisibleForTesting PurchasesUpdatedListener purchasesUpdatedListener;
    final private Handler mainHandler;

    private boolean clientConnected;
    private Queue<Runnable> serviceRequests = new ConcurrentLinkedQueue<>();

    BillingWrapper(ClientFactory clientFactory, Handler mainHandler) {
        billingClient = clientFactory.buildClient(this);
        this.mainHandler = mainHandler;
    }

    void setListener(@Nullable PurchasesUpdatedListener purchasesUpdatedListener) {
        this.purchasesUpdatedListener = purchasesUpdatedListener;
        if (purchasesUpdatedListener != null) {
            billingClient.startConnection(this);
        } else {
            billingClient.endConnection();
        }
    }

    private void executePendingRequests() {
        while (clientConnected && !serviceRequests.isEmpty()) {
            Runnable request = serviceRequests.remove();
            request.run();
        }
    }

    private void executeRequest(final Runnable request) {
        if (purchasesUpdatedListener != null) {
            serviceRequests.add(request);
            if (!clientConnected) {
                billingClient.startConnection(this);
            } else {
                executePendingRequests();
            }
        } else {
            Log.e("Purchases", "There is no listener set. Skipping. " +
                    "Make sure you set a listener before calling anything else.");
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
                    public void onSkuDetailsResponse(int responseCode, @Nullable List<SkuDetails> skuDetailsList) {
                        if (skuDetailsList == null) {
                            skuDetailsList = new ArrayList<>();
                        }
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
                BillingFlowParams.Builder builder = BillingFlowParams.newBuilder()
                        .setSku(sku)
                        .setType(skuType)
                        .setAccountId(appUserID);

                if (oldSkus.size() > 0) {
                    builder.setOldSkus(oldSkus);
                }

                BillingFlowParams params = builder.build();

                @BillingClient.BillingResponse int response = billingClient.launchBillingFlow(activity, params);
                if (response != BillingClient.BillingResponse.OK) {
                    Log.e("Purchases", "Failed to launch billing intent " + response);
                }
            }
        });
    }

    public void queryPurchaseHistoryAsync(final @BillingClient.SkuType String skuType,
                                          final PurchaseHistoryResponseListener listener) {
        executeRequest(new Runnable() {
            @Override
            public void run() {
                billingClient.queryPurchaseHistoryAsync(skuType, new com.android.billingclient.api.PurchaseHistoryResponseListener() {
                    @Override
                    public void onPurchaseHistoryResponse(@BillingClient.BillingResponse int responseCode, List<Purchase> purchasesList) {
                        if (responseCode == BillingClient.BillingResponse.OK) {
                            listener.onReceivePurchaseHistory(purchasesList);
                        } else {
                            listener.onReceivePurchaseHistoryError(responseCode, "Error receiving purchase history");
                        }
                    }
                });
            }
        });
    }

    public void consumePurchase(final String token) {
        executeRequest(new Runnable() {
            @Override
            public void run() {
                billingClient.consumeAsync(token, new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(int responseCode, String purchaseToken) {}
                });
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            purchasesUpdatedListener.onPurchasesUpdated(purchases);
        } else {
            if (purchases == null && responseCode == BillingClient.BillingResponse.OK) {
                responseCode = BillingClient.BillingResponse.ERROR;
            }
            purchasesUpdatedListener.onPurchasesFailedToUpdate(responseCode, "Error updating purchases " + responseCode);
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
