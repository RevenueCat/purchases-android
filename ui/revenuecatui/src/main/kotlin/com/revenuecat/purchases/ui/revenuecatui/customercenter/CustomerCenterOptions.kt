package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.customercenter.CustomerCenterListener

public class CustomerCenterOptions internal constructor(
    internal val listener: CustomerCenterListener?,
    internal val shouldShowCloseButton: Boolean = true,
) {

    internal constructor(builder: Builder) : this(
        listener = builder.listener,
        shouldShowCloseButton = builder.shouldShowCloseButton,
    )

    public class Builder {

        internal var listener: CustomerCenterListener? = null
        internal var shouldShowCloseButton: Boolean = true

        public fun setListener(listener: CustomerCenterListener?): Builder = apply {
            this.listener = listener
        }

        /**
         * Whether to show the close button in the Customer Center top bar.
         *
         * When `true` (the default), a close button is displayed that dismisses the Customer Center.
         * When `false`, the close button is hidden. This is useful when the Customer Center is pushed onto an
         * existing navigation stack that already provides a way to navigate back.
         *
         * Note this only affects the close button shown on the main screen. The back button used to navigate
         * between Customer Center screens is always displayed.
         */
        public fun setShouldShowCloseButton(shouldShowCloseButton: Boolean): Builder = apply {
            this.shouldShowCloseButton = shouldShowCloseButton
        }

        public fun build(): CustomerCenterOptions {
            return CustomerCenterOptions(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomerCenterOptions

        if (listener != other.listener) return false
        if (shouldShowCloseButton != other.shouldShowCloseButton) return false

        return true
    }

    override fun hashCode(): Int {
        var result = listener?.hashCode() ?: 0
        result = 31 * result + shouldShowCloseButton.hashCode()
        return result
    }

    override fun toString(): String {
        return "CustomerCenterOptions(listener=$listener, shouldShowCloseButton=$shouldShowCloseButton)"
    }
}
