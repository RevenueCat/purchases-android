@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class AdTrackerAdMobApiContractTest {

    // AdTracker loadAndTrack* extensions are @JvmSynthetic (Kotlin-only for now). No Java contract test.

    @Test
    fun `banner extension jvm class exposes static overloads`() {
        val bannerExtensionsClass = Class.forName("com.revenuecat.purchases.admob.RCAdMobBannerAd")

        val overloads = staticOverloads(bannerExtensionsClass, "loadAndTrackAd")
        assertTrue(
            "Expected at least one static public loadAndTrackAd method on ${bannerExtensionsClass.name}",
            overloads.isNotEmpty(),
        )
    }

    @Test
    fun `native extension jvm class exposes static overloads`() {
        val nativeExtensionsClass = Class.forName("com.revenuecat.purchases.admob.RCAdMobNativeAd")

        val overloads = staticOverloads(nativeExtensionsClass, "forNativeAdWithTracking")
        assertTrue(
            "Expected at least one static public forNativeAdWithTracking method on ${nativeExtensionsClass.name}",
            overloads.isNotEmpty(),
        )
    }

    private fun staticOverloads(clazz: Class<*>, methodName: String) = clazz.methods.filter { method ->
        method.name == methodName &&
            Modifier.isPublic(method.modifiers) &&
            Modifier.isStatic(method.modifiers) &&
            !method.isSynthetic
    }
}
