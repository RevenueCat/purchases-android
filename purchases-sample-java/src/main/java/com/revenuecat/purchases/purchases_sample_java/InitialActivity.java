package com.revenuecat.purchases.purchases_sample_java;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;
import com.revenuecat.sample.R;

public class InitialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Purchases.getSharedInstance().getPurchaserInfo(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get("pro_cat");
                if (proEntitlement != null && proEntitlement.isActive()) {
                    Navigator.startCatsActivity(InitialActivity.this, true);
                } else {
                    Navigator.startUpsellActivity(InitialActivity.this, true);
                }
                finish();
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e("Purchases Sample", error.getMessage());
            }
        });
    }
}
