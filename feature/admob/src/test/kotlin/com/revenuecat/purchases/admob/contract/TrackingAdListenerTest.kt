package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdListener
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Reflection-based test that verifies [TrackingAdListener] overrides every
 * public, non-final, non-static method declared in [AdListener].
 *
 * If Google adds new methods to [AdListener], this test will fail, alerting us
 * to update our wrapper so we don't silently drop new callbacks.
 */
class TrackingAdListenerTest {

    @Test
    fun `all overridable AdListener methods are overridden`() {
        val overridableMethods = AdListener::class.java.declaredMethods
            .filter {
                Modifier.isPublic(it.modifiers) &&
                    !Modifier.isStatic(it.modifiers) &&
                    !Modifier.isFinal(it.modifiers)
            }
            .map { it.name to it.parameterTypes.toList() }
            .toSet()

        val overriddenMethods = TrackingAdListener::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .map { it.name to it.parameterTypes.toList() }
            .toSet()

        val missing = overridableMethods - overriddenMethods

        assertTrue(
            "TrackingAdListener is missing overrides for: " +
                missing.joinToString { "${it.first}(${it.second.joinToString { c -> c.simpleName }})" },
            missing.isEmpty(),
        )
    }
}
