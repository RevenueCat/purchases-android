package com.revenuecat.purchases.admob

import com.google.android.gms.ads.FullScreenContentCallback
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Reflection-based test that verifies [TrackingFullScreenContentCallback] overrides
 * every public, non-final, non-static method declared in [FullScreenContentCallback].
 *
 * If Google adds new methods to [FullScreenContentCallback], this test will fail,
 * alerting us to update our wrapper so we don't silently drop new callbacks.
 */
class TrackingFullScreenContentCallbackTest {

    @Test
    fun `all overridable FullScreenContentCallback methods are overridden`() {
        val overridableMethods = FullScreenContentCallback::class.java.declaredMethods
            .filter {
                Modifier.isPublic(it.modifiers) &&
                    !Modifier.isStatic(it.modifiers) &&
                    !Modifier.isFinal(it.modifiers)
            }
            .map { it.name to it.parameterTypes.toList() }
            .toSet()

        val overriddenMethods = TrackingFullScreenContentCallback::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .map { it.name to it.parameterTypes.toList() }
            .toSet()

        val missing = overridableMethods - overriddenMethods

        assertTrue(
            "TrackingFullScreenContentCallback is missing overrides for: " +
                missing.joinToString { "${it.first}(${it.second.joinToString { c -> c.simpleName }})" },
            missing.isEmpty(),
        )
    }
}
