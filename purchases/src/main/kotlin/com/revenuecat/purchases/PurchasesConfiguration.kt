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
    val dangerousSettings: Set<DangerousSetting>

    init {
        this.context = builder.context
        this.apiKey = builder.apiKey
        this.appUserID = builder.appUserID
        this.observerMode = builder.observerMode
        this.service = builder.service
        this.store = builder.store
        this.dangerousSettings = builder.dangerousSettings
    }

    open class Builder(
        internal val context: Context,
        internal val apiKey: String
    ) {

        internal var appUserID: String? = null
        internal var observerMode: Boolean = false
        internal var service: ExecutorService? = null
        internal var store: Store = Store.PLAY_STORE
        internal var dangerousSettings: Set<DangerousSetting> = emptySet()

        fun appUserID(appUserID: String?) = apply {
            this.appUserID = appUserID
        }

        fun observerMode(observerMode: Boolean) = apply {
            this.observerMode = observerMode
        }

        fun service(service: ExecutorService) = apply {
            this.service = service
        }

        // TODO: make public
        internal fun store(store: Store) = apply {
            this.store = store
        }

        /**
         * Only use a Dangerous Setting if suggested by RevenueCat support team.
         */
        fun dangerousSettings(dangerousSettings: Set<DangerousSetting>) = apply {
            this.dangerousSettings = dangerousSettings
        }

        open fun build(): PurchasesConfiguration {
            return PurchasesConfiguration(this)
        }
    }
}
