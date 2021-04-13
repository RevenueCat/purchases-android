package com.revenuecat.purchasetester.java;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener;
import com.revenuecat.purchases.models.ProductDetails;
import com.revenuecat.purchases.models.PurchaseDetails;

import static com.revenuecat.purchasetester.java.MainApplication.PREMIUM_ENTITLEMENT_ID;

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
        findViewById(R.id.skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigator.startCatsActivity(UpsellActivity.this, false);
            }
        });
        Purchases.getSharedInstance().setUpdatedPurchaserInfoListener(new UpdatedPurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                checkForProEntitlement(purchaserInfo);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsListener() {
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
            ProductDetails product = aPackage.getProduct();
            String loadedText = "Buy " + aPackage.getPackageType() + " - " + product.getPriceCurrencyCode() + " " + product.getPrice();
            button.setTag(loadedText);
            showLoading(button, false);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makePurchase(aPackage, button);
                }
            });
        } else {
            Log.e("Purchase Tester Java", "Error loading package");
        }
    }

    private void makePurchase(Package packageToPurchase, final Button button) {
        showLoading(button, true);
        Purchases.getSharedInstance().purchasePackage(this, packageToPurchase, new PurchaseCallback() {
            @Override
            public void onCompleted(PurchaseDetails purchase, PurchaserInfo purchaserInfo) {
                showLoading(button, false);
                checkForProEntitlement(purchaserInfo);
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                if (!userCancelled) {
                    Log.e("Purchase Tester Java", error.getMessage());
                }
            }
        });
    }

    private void checkForProEntitlement(PurchaserInfo purchaserInfo) {
        EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get(PREMIUM_ENTITLEMENT_ID);
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
