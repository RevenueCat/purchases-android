package com.revenuecat.purchasetester.java;

import static com.revenuecat.purchasetester.java.MainApplication.PREMIUM_ENTITLEMENT_ID;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;

public class InitialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo customerInfo) {
                EntitlementInfo proEntitlement = customerInfo.getEntitlements().get(PREMIUM_ENTITLEMENT_ID);
                if (proEntitlement != null && proEntitlement.isActive()) {
                    Navigator.startCatsActivity(InitialActivity.this, true);
                } else {
                    Navigator.startUpsellActivity(InitialActivity.this, true);
                }
                finish();
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e("Purchase Tester Java", error.getMessage());
            }
        });
    }
}
