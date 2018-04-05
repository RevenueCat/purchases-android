package com.revenuecat.purchases_sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.purchases = new Purchases.Builder(this, "LQmxAoIaaQaHpPiWJJayypBDhIpAZCZN", this)
                .appUserID("jerry").build();

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onCompletedPurchase(PurchaserInfo purchaserInfo) {

    }

    @Override
    public void onFailedPurchase(Exception reason) {

    }

    @Override
    public void onReceiveUpdatedPurchaserInfo(PurchaserInfo purchaserInfo) {
        Log.i("MainActivity", "Got new purchaser info: " + purchaserInfo.getActiveSubscriptions());
    }
}
