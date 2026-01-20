package com.revenuecat.purchases.galaxy

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache

internal object GalaxyBillingWrapperFactory {

    @OptIn(InternalRevenueCatAPI::class)
    @Suppress("ThrowsCount")
    fun createGalaxyBillingWrapper(
        stateProvider: PurchasesStateProvider,
        context: Context,
        billingMode: GalaxyBillingMode,
        deviceCache: DeviceCache,
    ): BillingAbstract {
        try {
            val wrapperClass = Class.forName("com.revenuecat.purchases.galaxy.GalaxyBillingWrapper")
            val constructor = wrapperClass.getDeclaredConstructor(
                PurchasesStateProvider::class.java,
                Context::class.java,
                GalaxyBillingMode::class.java,
                DeviceCache::class.java,
            )
            @Suppress("UNCHECKED_CAST")
            return constructor.newInstance(
                stateProvider,
                context,
                billingMode,
                deviceCache,
            ) as BillingAbstract
        } catch (e: ClassNotFoundException) {
            val error = NoClassDefFoundError(e.message)
            error.initCause(e)
            throw error
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException(
                "Failed to find GalaxyBillingWrapper constructor. " +
                    "Please ensure that you've declared a dependency on the purchases-galaxy module.",
                e,
            )
        } catch (e: ReflectiveOperationException) {
            throw IllegalStateException("Failed to create GalaxyBillingWrapper", e)
        }
    }
}
