package com.revenuecat.purchases.ui.revenuecatui.customercenter

public class CustomerCenterNavigationOptions internal constructor(
    internal val shouldShowTopBar: Boolean = true,
    internal val listener: CustomerCenterNavigationListener? = null,
) {

    internal constructor(builder: Builder) : this(
        shouldShowTopBar = builder.shouldShowTopBar,
        listener = builder.listener,
    )

    public class Builder {

        internal var shouldShowTopBar: Boolean = true
        internal var listener: CustomerCenterNavigationListener? = null

        /**
         * Whether to show the topbar in the Customer Center.
         *
         * Set to false when you want to use your own navigation toolbar.
         */
        public fun setShouldShowTopBar(shouldShowTopBar: Boolean): Builder = apply {
            this.shouldShowTopBar = shouldShowTopBar
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
