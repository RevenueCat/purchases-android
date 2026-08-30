package com.revenuecat.purchases.ui.revenuecatui.customercenter

public class CustomerCenterNavigationOptions internal constructor(
    internal val shouldShowNavigationButton: Boolean = true,
    internal val listener: CustomerCenterNavigationListener? = null,
) {

    internal constructor(builder: Builder) : this(
        shouldShowNavigationButton = builder.shouldShowNavigationButton,
        listener = builder.listener,
    )

    public class Builder {

        internal var shouldShowNavigationButton: Boolean = true
        internal var listener: CustomerCenterNavigationListener? = null

        /**
         * Whether to show the navigation button in the Customer Center top bar.
         *
         * Set to false when you want to use your own navigation toolbar.
         */
        public fun setShouldShowNavigationButton(shouldShowNavigationButton: Boolean): Builder = apply {
            this.shouldShowNavigationButton = shouldShowNavigationButton
        }

        /**
         * Sets a [CustomerCenterNavigationListener] that will be notified every time the Customer Center navigates
         * to a different screen.
         */
        public fun setListener(listener: CustomerCenterNavigationListener?): Builder = apply {
            this.listener = listener
        }

        public fun build(): CustomerCenterNavigationOptions {
            return CustomerCenterNavigationOptions(this)
        }
    }
}
