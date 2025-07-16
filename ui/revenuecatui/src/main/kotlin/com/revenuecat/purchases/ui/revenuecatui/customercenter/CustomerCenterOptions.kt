package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.customercenter.CustomerCenterListener

public class CustomerCenterOptions internal constructor(
    internal val listener: CustomerCenterListener?,
) {

    internal constructor(builder: Builder) : this(
        listener = builder.listener,
    )

    public class Builder {

        internal var listener: CustomerCenterListener? = null

        public fun setListener(listener: CustomerCenterListener?): Builder = apply {
            this.listener = listener
        }

        public fun build(): CustomerCenterOptions {
            return CustomerCenterOptions(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomerCenterOptions

        return listener == other.listener
    }

    override fun hashCode(): Int {
        return listener?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "CustomerCenterOptions(listener=$listener)"
    }
}
