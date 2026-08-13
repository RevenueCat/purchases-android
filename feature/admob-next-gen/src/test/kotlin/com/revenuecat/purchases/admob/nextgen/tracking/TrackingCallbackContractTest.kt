package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class TrackingCallbackContractTest {

    @Test
    fun `load callback overrides every SDK callback`() {
        assertOverridesAllSdkCallbacks(AdLoadCallback::class.java, TrackingAdLoadCallback::class.java)
    }

    @Test
    fun `banner refresh callback overrides every SDK callback`() {
        assertOverridesAllSdkCallbacks(BannerAdRefreshCallback::class.java, TrackingBannerAdRefreshCallback::class.java)
    }
}

private fun assertOverridesAllSdkCallbacks(sdkCallback: Class<*>, trackingCallback: Class<*>) {
    val sdkMethods = sdkCallback.methods
        .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
        .map { it.signature() }
        .toSet()
    val trackingMethods = generateSequence(trackingCallback) { it.superclass }
        .flatMap { it.declaredMethods.asSequence() }
        .filter { Modifier.isPublic(it.modifiers) }
        .map { it.signature() }
        .toSet()
    val missing = sdkMethods - trackingMethods

    assertTrue(
        "${trackingCallback.simpleName} is missing overrides for: ${missing.joinToString()}",
        missing.isEmpty(),
    )
}

private fun Method.signature(): String = "$name(${parameterTypes.joinToString { it.name }})"
