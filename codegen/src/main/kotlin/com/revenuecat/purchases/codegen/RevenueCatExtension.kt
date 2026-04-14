package com.revenuecat.purchases.codegen

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class RevenueCatExtension @Inject public constructor(objects: ObjectFactory) {

    private companion object {
        private const val DEFAULT_CACHE_TTL_MINUTES = 30L
    }

    public val apiKey: Property<String> = objects.property(String::class.java)

    public val projectId: Property<String> = objects.property(String::class.java)

    /**
     * The package name for generated code. **Must be set explicitly** to a package you own
     * (e.g. `"com.myapp.rc"`). Avoid `com.revenuecat.*` packages — the RevenueCat SDK's
     * ProGuard/R8 `-keep class com.revenuecat.**` rule would prevent the generated code from
     * being minimized in release builds.
     */
    public val packageName: Property<String> = objects.property(String::class.java)

    /**
     * How long (in minutes) the fetched schema is cached before the plugin re-fetches from the
     * RevenueCat API. Defaults to 30 minutes.
     *
     * Set to `0` to force a fresh fetch on every build (useful in CI or when iterating on your
     * RevenueCat dashboard).
     */
    public val cacheTtlMinutes: Property<Long> = objects.property(Long::class.java)
        .convention(DEFAULT_CACHE_TTL_MINUTES)

    public val offlineMode: Property<OfflineMode> = objects.property(OfflineMode::class.java)
        .convention(OfflineMode.USE_CACHE_OR_SKIP)

    public val namingStyle: Property<NamingStyle> = objects.property(NamingStyle::class.java)
        .convention(NamingStyle.CAMEL_CASE)

    public val generateEntitlements: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val generateOfferings: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val generatePackages: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    public val generateCustomerInfoExtensions: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)
}
