package com.revenuecat.purchases.admob.nextgen.tracking

import org.junit.Assert.assertTrue
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun assertOverridesAllSdkCallbacks(sdkCallback: Class<*>, trackingCallback: Class<*>) {
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
