package com.revenuecat.purchases

import android.content.Context
import java.util.concurrent.ExecutorService

open class PurchasesConfiguration(builder: Builder) {

    val context: Context
    val apiKey: String
    val appUserID: String?
    val observerMode: Boolean
    val service: ExecutorService?
    val store: Store

    init {
        this.context = builder.context
        this.apiKey = builder.apiKey
        this.appUserID = builder.appUserID
        this.observerMode = builder.observerMode
        this.service = builder.service
        this.store = builder.store
    }

    open class Builder(
        @get:JvmSynthetic internal val context: Context,
        @get:JvmSynthetic internal val apiKey: String
    ) {

        @set:JvmSynthetic @get:JvmSynthetic internal var appUserID: String? = null
        @set:JvmSynthetic @get:JvmSynthetic internal var observerMode: Boolean = false
        @set:JvmSynthetic @get:JvmSynthetic internal var service: ExecutorService? = null
        @set:JvmSynthetic @get:JvmSynthetic internal var store: Store = Store.PLAY_STORE

        fun appUserID(appUserID: String?) = apply {
            this.appUserID = appUserID
        }

        fun observerMode(observerMode: Boolean) = apply {
            this.observerMode = observerMode
        }

        fun service(service: ExecutorService) = apply {
            this.service = service
        }

        fun store(store: Store) = apply {
            this.store = store
        }

        open fun build(): PurchasesConfiguration {
            return PurchasesConfiguration(this)
        }
    }
}
