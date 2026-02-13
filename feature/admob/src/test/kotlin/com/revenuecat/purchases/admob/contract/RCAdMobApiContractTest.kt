@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class RCAdMobApiContractTest {

    @Test
    fun `object exposes static overloads for all loadAndTrack entry points`() {
        val rcAdMobClass = RCAdMob::class.java

        assertStaticOverloadCount(rcAdMobClass, "loadAndTrackInterstitialAd", expected = 5)
        assertStaticOverloadCount(rcAdMobClass, "loadAndTrackAppOpenAd", expected = 5)
        assertStaticOverloadCount(rcAdMobClass, "loadAndTrackRewardedAd", expected = 5)
        assertStaticOverloadCount(rcAdMobClass, "loadAndTrackRewardedInterstitialAd", expected = 5)
        assertStaticOverloadCount(rcAdMobClass, "loadAndTrackNativeAd", expected = 6)
        assertStaticOverloadCount(rcAdMobClass, "loadAndTrackBannerAd", expected = 4)
    }

    @Test
    fun `banner extension jvm class exposes static overloads`() {
        val bannerExtensionsClass = Class.forName("com.revenuecat.purchases.admob.RCAdMobBannerAd")

        val overloads = staticOverloads(bannerExtensionsClass, "loadAndTrackAd")
        assertTrue(
            "Expected at least one static public loadAndTrackAd method on ${bannerExtensionsClass.name}",
            overloads.isNotEmpty(),
        )
    }

    private fun assertStaticOverloadCount(clazz: Class<*>, methodName: String, expected: Int) {
        val overloads = staticOverloads(clazz, methodName)

        assertTrue(
            "Expected at least one static public overload for $methodName on ${clazz.name}",
            overloads.isNotEmpty(),
        )
        assertEquals(
            "Unexpected static overload count for $methodName on ${clazz.name}",
            expected,
            overloads.size,
        )
    }

    private fun staticOverloads(clazz: Class<*>, methodName: String) = clazz.methods.filter { method ->
        method.name == methodName &&
            Modifier.isPublic(method.modifiers) &&
            Modifier.isStatic(method.modifiers) &&
            !method.isSynthetic
    }
}
