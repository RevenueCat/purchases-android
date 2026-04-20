package com.revenuecat.purchases.customercenter

public fun interface Resumable {
    public fun resume(shouldResume: Boolean)

    public operator fun invoke(shouldResume: Boolean = true): Unit = resume(shouldResume)
}
