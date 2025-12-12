/**
 * This class is in Java because there is a weird issue when written in Kotlin. The class
 * can't be used as a PurchasesConfiguration.Builder from a Java class when written in Kotlin.
 *
 * If written in Kotlin this wouldn't work:
 *
 * PurchasesConfiguration.Builder builder = new GalaxyConfiguration.Builder(this, "");
 *
 * And this either:
 *
 * GalaxyConfiguration.Builder builder = new GalaxyConfiguration.Builder(this, "");
 * builder.build();
 */
package com.revenuecat.purchases.galaxy;

import android.content.Context;

import androidx.annotation.NonNull;

import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.Store;

/**
 * Holds parameters to initialize the SDK for the Galaxy Store. Create an instance of this class using the [Builder]
 * and pass it to [Purchases.configure].
 */
public final class GalaxyConfiguration extends PurchasesConfiguration {

    public GalaxyConfiguration(@NonNull Builder builder) {
        super(builder);
    }

    public static final class Builder extends PurchasesConfiguration.Builder {

        public Builder(
                @NonNull Context context,
                @NonNull String apiKey
        ) {
            super(context, apiKey);
            this.store(Store.GALAXY);
            this.galaxyBillingMode(GalaxyBillingMode.PRODUCTION);
        }

        public Builder(
                @NonNull Context context,
                @NonNull String apiKey,
                @NonNull GalaxyBillingMode galaxyBillingMode
        ) {
            super(context, apiKey);
            this.store(Store.GALAXY);
            this.galaxyBillingMode(galaxyBillingMode);
        }

    }
}
