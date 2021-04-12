package com.revenuecat.purchasetester.java;

import android.app.Application;
import android.os.StrictMode;

import com.revenuecat.purchases.Purchases;

public class MainApplication extends Application {

    public static final String PREMIUM_ENTITLEMENT_ID = "pro_cat";

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        Purchases.setDebugLogsEnabled(true);
        Purchases.configure(this, "api_key");
    }

}
