package com.revenuecat.purchases.codegen

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class RevenueCatExtension @Inject public constructor(objects: ObjectFactory) {

    public val apiKey: Property<String> = objects.property(String::class.java)

    public val projectId: Property<String> = objects.property(String::class.java)

    public val packageName: Property<String> = objects.property(String::class.java)
        .convention("com.revenuecat.generated")

    public val cacheTtlMinutes: Property<Long> = objects.property(Long::class.java)
        .convention(60L)

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
