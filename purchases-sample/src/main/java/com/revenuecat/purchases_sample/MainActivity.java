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
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Purchases.PurchasesListener {

    private Purchases purchases;
    private final Map<String, SkuDetails> skuDetailsByIdentifier = new HashMap<>();
    private Button mButton;
    private RecyclerView mRecyclerView;

    private static final String ONEMONTH_TRIAL_SKU = "onemonth_freetrial";
    private LinearLayoutManager mLayoutManager;

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
            String dateString = expiration.toString();

            Boolean expired = expiration.before(new Date());

            String expiredIcon = expired ? "❌" : "✅";

            String message = key + " " + expiredIcon + " " + dateString;
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

        this.purchases = new Purchases.Builder(this, "LQmxAoIaaQaHpPiWJJayypBDhIpAZCZN", this)
                .appUserID("jerry1001").build();

        List<String> skus = new ArrayList<>();
        skus.add(ONEMONTH_TRIAL_SKU);

        this.purchases.getSubscriptionSkus(skus, new Purchases.GetSkusResponseHandler() {
            @Override
            public void onReceiveSkus(List<SkuDetails> skus) {
                Log.d("Purchases", "Got skus " + skus.get(0));


                for (SkuDetails details : skus) {
                    skuDetailsByIdentifier.put(details.getSku(), details);
                }


                if (skuDetailsByIdentifier.containsKey(ONEMONTH_TRIAL_SKU)) {
                    SkuDetails details = skuDetailsByIdentifier.get(ONEMONTH_TRIAL_SKU);
                    mButton.setText("Buy One Month w/ Trial - " + details.getPrice());
                    mButton.setEnabled(true);
                }
            }
        });

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (skuDetailsByIdentifier.containsKey(ONEMONTH_TRIAL_SKU)) {
                    SkuDetails details = skuDetailsByIdentifier.get(ONEMONTH_TRIAL_SKU);
                    purchases.makePurchase(MainActivity.this, details.getSku(), details.getType());
                }
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

        mRecyclerView = (RecyclerView) findViewById(R.id.expirationDates);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    @Override
    public void onCompletedPurchase(String sku, PurchaserInfo purchaserInfo) {
        Log.i("Purchases", "Purchase completed: " + purchaserInfo);
        onReceiveUpdatedPurchaserInfo(purchaserInfo);
    }

    @Override
    public void onFailedPurchase(Exception reason) {
        Log.i("Purchases", "" + reason);
    }

    @Override
    public void onReceiveUpdatedPurchaserInfo(final PurchaserInfo purchaserInfo) {
        Log.i("Purchases", "Got new purchaser info: " + purchaserInfo.getActiveSubscriptions());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(new ExpirationsAdapter(purchaserInfo.getAllExpirationDates()));
                mRecyclerView.invalidate();
            }
        });
    }
}
