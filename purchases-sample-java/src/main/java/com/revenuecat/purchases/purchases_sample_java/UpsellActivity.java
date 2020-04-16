package com.revenuecat.purchases.purchases_sample_java;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.MakePurchaseListener;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener;
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener;
import com.revenuecat.sample.R;

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
                Log.e("Purchases Sample", error.getMessage());
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
                setupMonthlyButton(currentOffering.getMonthly());
                setupAnnualButton(currentOffering.getAnnual());
                setupUnlimitedButton(currentOffering.getLifetime());
            } else {
                Log.e("Purchases Sample", "Error loading current offering");
            }
        }
    }

    private void setupUnlimitedButton(@Nullable final Package lifetimePackage) {
        if (lifetimePackage != null) {
            SkuDetails unlimitedProduct = lifetimePackage.getProduct();
            String loadedText = "Buy Unlimited - " + unlimitedProduct.getPriceCurrencyCode() + " " + unlimitedProduct.getPrice();
            unlimitedPurchaseView.setTag(loadedText);
            showLoading(unlimitedPurchaseView, false);
            unlimitedPurchaseView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makePurchase(lifetimePackage, unlimitedPurchaseView);
                }
            });
        } else {
            Log.e("Purchases Sample", "Error loading lifetime package");
        }
    }

    private void setupMonthlyButton(@Nullable final Package monthlyPackage) {
        if (monthlyPackage != null) {
            SkuDetails monthlyProduct = monthlyPackage.getProduct();
            String loadedText = "Buy Monthly - " + monthlyProduct.getPriceCurrencyCode() + " " + monthlyProduct.getPrice();
            monthlyPurchaseView.setTag(loadedText);
            showLoading(monthlyPurchaseView, false);
            monthlyPurchaseView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makePurchase(monthlyPackage, monthlyPurchaseView);
                }
            });
        } else {
            Log.e("Purchases Sample", "Error loading monthly package");
        }
    }

    private void setupAnnualButton(@Nullable final Package annualPackage) {
        if (annualPackage != null) {
            SkuDetails annualProduct = annualPackage.getProduct();
            String loadedText = "Buy Annual - " + annualProduct.getPriceCurrencyCode() + " " + annualProduct.getPrice();
            annualPurchaseView.setTag(loadedText);
            showLoading(annualPurchaseView, false);
            annualPurchaseView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makePurchase(annualPackage, annualPurchaseView);
                }
            });
        } else {
            Log.e("Purchases Sample", "Error loading annual package");
        }
    }

    private void makePurchase(Package packageToPurchase, final Button button) {
        showLoading(button, true);
        Purchases.getSharedInstance().purchasePackage(this, packageToPurchase, new MakePurchaseListener() {
            @Override
            public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo) {
                showLoading(button, false);
                checkForProEntitlement(purchaserInfo);
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                if (!userCancelled) {
                    Log.e("Purchases Sample", error.getMessage());
                }
            }
        });
    }

    private void checkForProEntitlement(PurchaserInfo purchaserInfo) {
        EntitlementInfo proEntitlement = purchaserInfo.getEntitlements().get("pro_cat");
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
