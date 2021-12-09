package com.revenuecat.purchasetester.java;

import static com.revenuecat.purchasetester.java.MainApplication.PREMIUM_ENTITLEMENT_ID;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;

import java.text.DateFormat;
import java.util.Date;

public class CatsActivity extends AppCompatActivity {
    Button restoreView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cats);

        findViewById(R.id.go_premium).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        restoreView = findViewById(R.id.purchase_restore);
        restoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restoreView.setText("Loading...");
                Purchases.getSharedInstance().restorePurchases(new ReceiveCustomerInfoCallback() {
                    @Override
                    public void onReceived(@NonNull CustomerInfo customerInfo) {
                        restoreView.setText("Restore Purchases");
                        configureContent(customerInfo);
                    }

                    @Override
                    public void onError(@NonNull PurchasesError error) {
                        Log.e("Purchase Tester Java", error.getMessage());
                    }
                });
            }
        });
    }

    private void configureContent(final CustomerInfo customerInfo) {
        TextView purchaseDateView = findViewById(R.id.purchase_date_label);
        TextView expirationDateView = findViewById(R.id.expiration_date_label);
        TextView catIconView = findViewById(R.id.cat_content_label);
        View goPremiumView = findViewById(R.id.go_premium);

        EntitlementInfo proEntitlement = customerInfo.getEntitlements().get(PREMIUM_ENTITLEMENT_ID);
        if (proEntitlement != null && proEntitlement.isActive()) {
            Log.i("Purchase Tester Java", "Hey there premium, you're a happy cat 😻");
            catIconView.setText("😻");
            goPremiumView.setVisibility(View.GONE);
            restoreView.setVisibility(View.GONE);

            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());

            Date purchaseDate = customerInfo.getPurchaseDateForEntitlement(PREMIUM_ENTITLEMENT_ID);
            purchaseDateView.setText("Purchase Date: " + dateFormat.format(purchaseDate));
            purchaseDateView.setVisibility(View.VISIBLE);

            Date expirationDate = customerInfo.getExpirationDateForEntitlement(PREMIUM_ENTITLEMENT_ID);
            expirationDateView.setText("Expiration Date: " + dateFormat.format(expirationDate));
            expirationDateView.setVisibility(View.VISIBLE);
        } else {
            Log.i("Purchase Tester Java", "Happy cats are only for premium members 😿");
            catIconView.setText("😿");
            goPremiumView.setVisibility(View.VISIBLE);
            restoreView.setVisibility(View.VISIBLE);
            expirationDateView.setVisibility(View.GONE);
            purchaseDateView.setVisibility(View.GONE);
        }

        Button manageSubscriptionButton = findViewById(R.id.manage_subscription);

        if (customerInfo.getManagementURL() != null) {
            manageSubscriptionButton.setVisibility(View.VISIBLE);
            manageSubscriptionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, customerInfo.getManagementURL()));
                    } catch (ActivityNotFoundException exception) {
                        exception.printStackTrace();
                    }
                }
            });
        } else {
            manageSubscriptionButton.setVisibility(View.GONE);
        }
    }

}
