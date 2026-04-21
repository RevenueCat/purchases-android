package com.revenuecat.purchases.galaxy

import android.content.Context
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache

internal object GalaxyBillingWrapperFactory {

    @OptIn(InternalRevenueCatAPI::class, ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
            val wrapperInstance = constructor.newInstance(
                stateProvider,
                context,
                billingMode,
                deviceCache,
            )
            check(wrapperInstance is BillingAbstract) {
                "GalaxyBillingWrapper does not implement BillingAbstract"
            }
            return wrapperInstance
        } catch (e: ClassNotFoundException) {
            handleMissingGalaxyModule(e)
        } catch (e: NoSuchMethodException) {
            handleMissingConstructor(e)
        } catch (e: ReflectiveOperationException) {
            handleWrapperCreationFailure(e)
        }
    }

    private fun handleMissingGalaxyModule(e: ClassNotFoundException): Nothing {
        val error = NoClassDefFoundError(e.message)
        error.initCause(e)
        throw error
    }

    private fun handleMissingConstructor(e: NoSuchMethodException): Nothing {
        throw IllegalStateException(
            "Failed to find GalaxyBillingWrapper constructor. " +
                "Please ensure that you've declared a dependency on the purchases-galaxy module.",
            e,
        )
    }

    private fun handleWrapperCreationFailure(e: ReflectiveOperationException): Nothing {
        throw IllegalStateException("Failed to create GalaxyBillingWrapper", e)
    }
}
