package com.revenuecat.purchases_sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Purchases.PurchasesListener {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    private Purchases purchases;
    private SkuDetails skuDetails;
    private Button mButton;
    private TextView mPurchaserInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.purchases = new Purchases.Builder(this, "LQmxAoIaaQaHpPiWJJayypBDhIpAZCZN", this)
                .appUserID("jerry").build();

        List<String> skus = new ArrayList<>();
        skus.add("onemonth_freetrial");

        this.purchases.getSubscriptionSkus(skus, new Purchases.GetSkusResponseHandler() {
            @Override
            public void onReceiveSkus(List<SkuDetails> skus) {
                Log.d("Purchases", "Got skus " + skus.get(0));
                skuDetails = skus.get(0);
                mButton.setEnabled(true);
            }
        });

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (skuDetails != null) {
                    purchases.makePurchase(MainActivity.this, skuDetails.getSku(), skuDetails.getType());
                }
            }
        });
        mButton.setEnabled(false);

        mPurchaserInfo = (TextView) findViewById(R.id.purchaserInfoTextView);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onCompletedPurchase(PurchaserInfo purchaserInfo) {
        Log.i("Purchases", "Purchase completed: " + purchaserInfo);
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
                mPurchaserInfo.setText(purchaserInfo.getActiveSubscriptions().toString());
            }
        });
    }
}
