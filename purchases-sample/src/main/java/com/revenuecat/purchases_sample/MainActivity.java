package com.revenuecat.purchases_sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.Entitlement;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Purchases.PurchasesListener {

    private Purchases purchases;
    private SkuDetails monthlySkuDetails;
    private Button mButton;
    private RecyclerView mRecyclerView;

    private LinearLayoutManager mLayoutManager;
    private Map<String, Entitlement> entitlementMap;

    public class ExpirationsAdapter extends RecyclerView.Adapter<ExpirationsAdapter.ViewHolder> {
        private final Map<String, Date> mExpirationDates;
        private final ArrayList<String> mSortedKeys;

        public ExpirationsAdapter(Map<String, Date> expirationDates) {
            mExpirationDates = expirationDates;
            mSortedKeys = new ArrayList<>(expirationDates.keySet());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String key = mSortedKeys.get(position);
            Date expiration = mExpirationDates.get(key);

            Boolean active = expiration == null || expiration.after(new Date());

            String expiredIcon = active ? "✅" : "❌";

            String message = key + " " + expiredIcon + " " + expiration;
            holder.mTextView.setText(message);
        }

        @Override
        public int getItemCount() {
            return mSortedKeys.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;
            ViewHolder(TextView view) {
                super(view);
                mTextView = view;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.expirationDates);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        this.purchases = new Purchases.Builder(this, "LQmxAoIaaQaHpPiWJJayypBDhIpAZCZN", this)
                .appUserID("jerry").build();

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchases.makePurchase(MainActivity.this, monthlySkuDetails.getSku(), monthlySkuDetails.getType());
            }
        });
        mButton.setEnabled(false);

        Button restoreButton = (Button)findViewById(R.id.restoreButton);
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchases.restorePurchasesForPlayStoreAccount();
            }
        });

        this.purchases.getEntitlements(new Purchases.GetEntitlementsHandler() {
            @Override
            public void onReceiveEntitlements(Map<String, Entitlement> entitlementMap) {
                Entitlement pro = entitlementMap.get("pro");
                Offering monthly = pro.getOfferings().get("monthly");

                MainActivity.this.entitlementMap = entitlementMap;

                monthlySkuDetails = monthly.getSkuDetails();

                mButton.setText("Buy One Month w/ Trial - " + monthlySkuDetails.getPrice());
                mButton.setEnabled(true);
            }

            @Override
            public void onReceiveEntitlementsError(int domain, int code, String message) {

            }
        });

    }

    @Override
    public void onCompletedPurchase(String sku, PurchaserInfo purchaserInfo) {
        Log.i("Purchases", "Purchase completed: " + purchaserInfo);
        onReceiveUpdatedPurchaserInfo(purchaserInfo);
    }

    @Override
    public void onFailedPurchase(int domain, int code, String reason) {
        Log.i("Purchases", reason);
    }

    @Override
    public void onReceiveUpdatedPurchaserInfo(final PurchaserInfo purchaserInfo) {
        Log.i("Purchases", "Got new purchaser info: " + purchaserInfo.getActiveSubscriptions());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(new ExpirationsAdapter(purchaserInfo.getAllExpirationDatesByEntitlement()));
                mRecyclerView.invalidate();
            }
        });
    }

    @Override
    public void onRestoreTransactions(final PurchaserInfo purchaserInfo) {
        Log.i("Purchases", "Got new purchaser info: " + purchaserInfo.getActiveSubscriptions());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(new ExpirationsAdapter(purchaserInfo.getAllExpirationDatesByEntitlement()));
                mRecyclerView.invalidate();
            }
        });
    }

    @Override
    public void onRestoreTransactionsFailed(int domain, int code, String reason) {
        Log.i("Purchases", reason);
    }
}
