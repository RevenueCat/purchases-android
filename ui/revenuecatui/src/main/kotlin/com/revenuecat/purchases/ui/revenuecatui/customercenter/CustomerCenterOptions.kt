package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.customercenter.CustomerCenterListener

public class CustomerCenterOptions internal constructor(
    internal val listener: CustomerCenterListener?,
    internal val navigationOptions: CustomerCenterNavigationOptions = CustomerCenterNavigationOptions(),
) {

    internal constructor(builder: Builder) : this(
        listener = builder.listener,
        navigationOptions = builder.navigationOptions,
    )

    public class Builder {

        internal var listener: CustomerCenterListener? = null
        internal var navigationOptions: CustomerCenterNavigationOptions = CustomerCenterNavigationOptions()

        public fun setListener(listener: CustomerCenterListener?): Builder = apply {
            this.listener = listener
        }

        /**
         * Sets the [CustomerCenterNavigationOptions] that configure how the Customer Center navigation integrates
         * with the app displaying it.
         */
        public fun setNavigationOptions(navigationOptions: CustomerCenterNavigationOptions): Builder = apply {
            this.navigationOptions = navigationOptions
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
        if (navigationOptions != other.navigationOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = listener?.hashCode() ?: 0
        result = 31 * result + navigationOptions.hashCode()
        return result
    }

    override fun toString(): String {
        return "CustomerCenterOptions(listener=$listener, navigationOptions=$navigationOptions)"
    }
}
