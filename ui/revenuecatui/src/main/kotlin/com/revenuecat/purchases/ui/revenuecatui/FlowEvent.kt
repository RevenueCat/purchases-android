package com.revenuecat.purchases.ui.revenuecatui

import dev.drewhamilton.poko.Poko

/**
 * A flow step or paywall lifecycle event, as passed to [PaywallListener.onFlowEvent].
 *
 * A flow is the whole paywall experience, from one step to many. A paywall shown without a flow reports only
 * [EventTypes.PAYWALL_IMPRESSION] and [EventTypes.PAYWALL_CLOSE].
 */
@Poko
public class FlowEvent internal constructor(
    /** The event as snake_case keys ([Key.name]) for analytics SDKs; keys that do not apply are absent. */
    public val rawProperties: Map<String, Any>,
) {

    /** The value for [key], or null when it does not apply to this event. */
    public fun <T : Any> getProperty(key: Key<T>): T? =
        rawProperties[key.name]?.takeIf(key.type::isInstance)?.let(key.type::cast)

    public class Key<T : Any> internal constructor(
        public val name: String,
        internal val type: Class<T>,
    )

    public object Keys {
        /** One of [EventTypes]. */
        @JvmField public val EVENT_TYPE: Key<String> = stringKey("event_type")

        /** Milliseconds since the epoch. */
        @JvmField public val TIMESTAMP: Key<Long> = longKey("timestamp")

        @JvmField public val FLOW_ID: Key<String> = stringKey("flow_id")

        @JvmField public val FLOW_NAME: Key<String> = stringKey("flow_name")

        @JvmField public val STEP_ID: Key<String> = stringKey("step_id")

        @JvmField public val STEP_TYPE: Key<String> = stringKey("step_type")

        @JvmField public val SCREEN_TYPE: Key<List<String>> = stringListKey("screen_type")

        @JvmField public val SCREEN_NAME: Key<String> = stringKey("screen_name")

        @JvmField public val TRACE_ID: Key<String> = stringKey("trace_id")

        @JvmField public val LOCALE: Key<String> = stringKey("locale")

        @JvmField public val EXPERIMENT_ID: Key<String> = stringKey("experiment_id")

        @JvmField public val EXPERIMENT_VARIANT: Key<String> = stringKey("experiment_variant")

        @JvmField public val FROM_STEP_ID: Key<String> = stringKey("from_step_id")

        @JvmField public val TO_STEP_ID: Key<String> = stringKey("to_step_id")

        @JvmField public val ENTRY_REASON: Key<String> = stringKey("entry_reason")

        @JvmField public val IS_FIRST_STEP: Key<Boolean> = booleanKey("is_first_step")

        @JvmField public val IS_LAST_STEP: Key<Boolean> = booleanKey("is_last_step")

        @JvmField public val PAYWALL_ID: Key<String> = stringKey("paywall_id")

        @JvmField public val OFFERING_ID: Key<String> = stringKey("offering_id")

        @JvmField public val PAYWALL_REVISION: Key<Int> = intKey("paywall_revision")

        @JvmField public val SESSION_ID: Key<String> = stringKey("session_id")

        @JvmField public val DISPLAY_MODE: Key<String> = stringKey("display_mode")

        @JvmField public val DARK_MODE: Key<Boolean> = booleanKey("dark_mode")

        private fun stringKey(name: String) = Key(name, String::class.java)
        private fun intKey(name: String) = Key(name, Int::class.javaObjectType)
        private fun longKey(name: String) = Key(name, Long::class.javaObjectType)
        private fun booleanKey(name: String) = Key(name, Boolean::class.javaObjectType)

        @Suppress("UNCHECKED_CAST")
        private fun stringListKey(name: String) = Key(name, List::class.java as Class<List<String>>)
    }

    /**
     * Known values of [Keys.EVENT_TYPE].
     */
    public object EventTypes {
        public const val FLOW_STEP_STARTED: String = "flow_step_started"
        public const val FLOW_STEP_COMPLETED: String = "flow_step_completed"
        public const val FLOW_CLOSE: String = "flow_close"
        public const val PAYWALL_IMPRESSION: String = "paywall_impression"
        public const val PAYWALL_CLOSE: String = "paywall_close"
    }
}
