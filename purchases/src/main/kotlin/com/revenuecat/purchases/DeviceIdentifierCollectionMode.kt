package com.revenuecat.purchases

import dev.drewhamilton.poko.Poko

/**
 * Which device identifiers the SDK collects on its own. Set it with
 * [PurchasesConfiguration.Builder.deviceIdentifierCollectionMode].
 *
 * [Purchases.collectDeviceIdentifiers] collects them in every mode.
 */
@Poko
public class DeviceIdentifierCollectionMode internal constructor(
    public val name: String,
) {

    override fun toString(): String = name

    public companion object {
        /**
         * When setting the identifier for an attribution network, for example with [Purchases.setAdjustID] or
         * [Purchases.setAppsflyerID], the SDK also collects the Android advertising ID, IP and device version,
         * if available, and sends them to RevenueCat. Some attribution networks need them to attribute installs
         * and re-installs. The advertising ID is not collected when the user has limited ad tracking.
         *
         * This is the default.
         */
        @JvmField
        public val AUTOMATIC: DeviceIdentifierCollectionMode = DeviceIdentifierCollectionMode("AUTOMATIC")

        /**
         * The SDK only collects device identifiers when you call [Purchases.collectDeviceIdentifiers].
         */
        @JvmField
        public val MANUAL_ONLY: DeviceIdentifierCollectionMode = DeviceIdentifierCollectionMode("MANUAL_ONLY")
    }
}
