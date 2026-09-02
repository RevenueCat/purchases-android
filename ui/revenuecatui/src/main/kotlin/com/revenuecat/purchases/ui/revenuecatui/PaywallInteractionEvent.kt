package com.revenuecat.purchases.ui.revenuecatui

/**
 * A paywall control interaction, as passed to [PaywallListener.onInteraction].
 */
public class PaywallInteractionEvent internal constructor(
    /** The interaction as snake_case keys ([Key.name]) for analytics SDKs; keys that do not apply are absent. */
    public val rawProperties: Map<String, Any>,
) {

    /** The value for [key], or null when it does not apply to this interaction. */
    public fun <T : Any> getProperty(key: Key<T>): T? =
        rawProperties[key.name]?.takeIf(key.type::isInstance)?.let(key.type::cast)

    override fun equals(other: Any?): Boolean =
        this === other || (other is PaywallInteractionEvent && other.rawProperties == rawProperties)

    override fun hashCode(): Int = rawProperties.hashCode()

    override fun toString(): String = "PaywallInteractionEvent($rawProperties)"

    public class Key<T : Any> internal constructor(
        public val name: String,
        internal val type: Class<T>,
    )

    public object Keys {
        /** Milliseconds since the epoch. */
        @JvmField public val TIMESTAMP: Key<Long> = longKey("timestamp")

        @JvmField public val SESSION_ID: Key<String> = stringKey("session_id")

        @JvmField public val OFFERING_ID: Key<String> = stringKey("offering_id")

        @JvmField public val PAYWALL_ID: Key<String> = stringKey("paywall_id")

        @JvmField public val PAYWALL_REVISION: Key<Int> = intKey("paywall_revision")

        @JvmField public val DISPLAY_MODE: Key<String> = stringKey("display_mode")

        @JvmField public val DARK_MODE: Key<Boolean> = booleanKey("dark_mode")

        @JvmField public val LOCALE: Key<String> = stringKey("locale")

        @JvmField public val COMPONENT_TYPE: Key<String> = stringKey("component_type")

        @JvmField public val COMPONENT_VALUE: Key<String> = stringKey("component_value")

        @JvmField public val COMPONENT_NAME: Key<String> = stringKey("component_name")

        @JvmField public val COMPONENT_URL: Key<String> = stringKey("component_url")

        @JvmField public val ORIGIN_INDEX: Key<Int> = intKey("origin_index")

        @JvmField public val DESTINATION_INDEX: Key<Int> = intKey("destination_index")

        @JvmField public val ORIGIN_CONTEXT_NAME: Key<String> = stringKey("origin_context_name")

        @JvmField public val DESTINATION_CONTEXT_NAME: Key<String> = stringKey("destination_context_name")

        @JvmField public val DEFAULT_INDEX: Key<Int> = intKey("default_index")

        @JvmField public val ORIGIN_PACKAGE_ID: Key<String> = stringKey("origin_package_id")

        @JvmField public val DESTINATION_PACKAGE_ID: Key<String> = stringKey("destination_package_id")

        @JvmField public val DEFAULT_PACKAGE_ID: Key<String> = stringKey("default_package_id")

        @JvmField public val CURRENT_PACKAGE_ID: Key<String> = stringKey("current_package_id")

        @JvmField public val RESULTING_PACKAGE_ID: Key<String> = stringKey("resulting_package_id")

        @JvmField public val ORIGIN_PRODUCT_ID: Key<String> = stringKey("origin_product_id")

        @JvmField public val DESTINATION_PRODUCT_ID: Key<String> = stringKey("destination_product_id")

        @JvmField public val DEFAULT_PRODUCT_ID: Key<String> = stringKey("default_product_id")

        @JvmField public val CURRENT_PRODUCT_ID: Key<String> = stringKey("current_product_id")

        @JvmField public val RESULTING_PRODUCT_ID: Key<String> = stringKey("resulting_product_id")

        private fun stringKey(name: String) = Key(name, String::class.java)
        private fun intKey(name: String) = Key(name, Int::class.javaObjectType)
        private fun longKey(name: String) = Key(name, Long::class.javaObjectType)
        private fun booleanKey(name: String) = Key(name, Boolean::class.javaObjectType)
    }

    /**
     * Known values of [Keys.COMPONENT_TYPE].
     */
    public object ComponentTypes {
        public const val TAB: String = "tab"
        public const val SWITCH: String = "switch"
        public const val CAROUSEL: String = "carousel"
        public const val BUTTON: String = "button"
        public const val TEXT: String = "text"
        public const val PACKAGE: String = "package"
        public const val PACKAGE_SELECTION_SHEET: String = "package_selection_sheet"
        public const val PURCHASE_BUTTON: String = "purchase_button"
    }
}
