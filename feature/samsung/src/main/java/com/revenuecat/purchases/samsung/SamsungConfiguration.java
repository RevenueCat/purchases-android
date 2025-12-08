/**
 * This class is in Java because there is a weird issue when written in Kotlin. The class
 * can't be used as a PurchasesConfiguration.Builder from a Java class when written in Kotlin.
 *
 * If written in Kotlin this wouldn't work:
 *
 * PurchasesConfiguration.Builder builder = new SamsungConfiguration.Builder(this, "");
 *
 * And this either:
 *
 * SamsungConfiguration.Builder builder = new SamsungConfiguration.Builder(this, "");
 * builder.build();
 */
package com.revenuecat.purchases.samsung;

import android.content.Context;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.Store;

/**
 * Holds parameters to initialize the SDK for the Samsung Store. Create an instance of this class using the [Builder]
 * and pass it to [Purchases.configure].
 */
public final class SamsungConfiguration extends PurchasesConfiguration {

    public SamsungConfiguration(@NonNull Builder builder) {
        super(builder);
    }

    public static final class Builder extends PurchasesConfiguration.Builder {

        public Builder(
                @NonNull Context context,
                @NonNull String apiKey,
                @NonNull SamsungBillingMode samsungBillingMode
        ) {
            super(context, apiKey);

            // TODO: Make this SAMSUNG after https://github.com/RevenueCat/purchases-android/pull/2900 is merged
            this.store(Store.AMAZON);
        }

    }
}
