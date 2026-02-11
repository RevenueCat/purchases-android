package com.revenuecat.purchases.virtualcurrencies

import android.os.Parcelable
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This class contains all the virtual currencies associated to the user.
 *
 * @property all - All of the virtual currencies associated to the user.
 */
@Poko
@Parcelize
@Serializable
public class VirtualCurrencies @InternalRevenueCatAPI constructor(
    @SerialName("virtual_currencies")
    public val all: Map<String, VirtualCurrency>,
) : Parcelable {

    /**
     * Returns the virtual currency for the given key, or null if it doesn't exist.
     *
     * @param code The code of the virtual currency to retrieve
     * @return The virtual currency, or null if not found
     */
    public operator fun get(code: String): VirtualCurrency? = all[code]
}
