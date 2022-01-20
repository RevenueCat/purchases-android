package com.revenuecat.purchasetester.java;

import static com.revenuecat.purchasetester.java.MainApplication.PREMIUM_ENTITLEMENT_ID;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.StoreTransaction;

public class UpsellActivity extends AppCompatActivity {

    Button monthlyPurchaseView;
    Button annualPurchaseView;
    Button unlimitedPurchaseView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upsell);

        monthlyPurchaseView = findViewById(R.id.monthly_purchase);
        annualPurchaseView = findViewById(R.id.annual_purchase);
        unlimitedPurchaseView = findViewById(R.id.unlimited_purchase);

        showScreen(null);
        findViewById(R.id.skip).setOnClickListener(v -> Navigator.startCatsActivity(
                UpsellActivity.this,
                false));
        Purchases.getSharedInstance().setUpdatedCustomerInfoListener(customerInfo ->
                checkForProEntitlement(customerInfo)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                showScreen(offerings);
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e("Purchase Tester Java", error.getMessage());
            }
        });
    }

    private void showScreen(@Nullable Offerings offerings) {
        if (offerings == null) {
            monthlyPurchaseView.setText("Loading...");
            monthlyPurchaseView.setEnabled(false);
            annualPurchaseView.setText("Loading...");
            annualPurchaseView.setEnabled(false);
        } else {
            Offering currentOffering = offerings.getCurrent();
            if (currentOffering != null) {
                setupPackageButton(currentOffering.getMonthly(), monthlyPurchaseView);
                setupPackageButton(currentOffering.getAnnual(), annualPurchaseView);
                setupPackageButton(currentOffering.getLifetime(), unlimitedPurchaseView);
            } else {
                Log.e("Purchase Tester Java", "Error loading current offering");
            }
        }
    }

    private void setupPackageButton(@Nullable final Package aPackage, final Button button) {
        if (aPackage != null) {
            StoreProduct product = aPackage.getProduct();
            String loadedText = "Buy " + aPackage.getPackageType() + " - " + product.getPriceCurrencyCode() + " " + product.getPrice();
            button.setTag(loadedText);
            showLoading(button, false);
            button.setOnClickListener(v -> makePurchase(aPackage, button));
        } else {
            Log.e("Purchase Tester Java", "Error loading package");
        }
    }

    private void makePurchase(Package packageToPurchase, final Button button) {
        showLoading(button, true);
        Purchases.getSharedInstance().purchasePackage(this, packageToPurchase, new PurchaseCallback() {
            @Override
            public void onCompleted(@NonNull StoreTransaction purchase, @NonNull CustomerInfo customerInfo) {
                showLoading(button, false);
                checkForProEntitlement(customerInfo);
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                if (!userCancelled) {
                    Log.e("Purchase Tester Java", error.getMessage());
                }
            }
        });
    }

    private void checkForProEntitlement(CustomerInfo customerInfo) {
        EntitlementInfo proEntitlement = customerInfo.getEntitlements().get(PREMIUM_ENTITLEMENT_ID);
        if (proEntitlement != null && proEntitlement.isActive()) {
            Navigator.startCatsActivity(UpsellActivity.this, false);
        }
    }

    private void showLoading(Button button, boolean loading) {
        String text;
        if (loading) {
            text = "Loading...";
        } else {
            text = (String) button.getTag();
        }
        button.setText(text);
        button.setEnabled(!loading);
    }

}
