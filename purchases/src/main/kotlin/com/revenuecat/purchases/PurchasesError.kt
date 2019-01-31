package com.revenuecat.purchases

import android.os.Parcel
import android.os.Parcelable

/**
 * This class represents an error
 * @param domain Domain of the error
 * @param code Error code
 * @param message Message explaining the error
 */
class PurchasesError(
    val domain: Purchases.ErrorDomains,
    val code: Int,
    val message: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Purchases.ErrorDomains::class.java.classLoader)!!,
        parcel.readInt(),
        parcel.readString()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchasesError

        if (domain != other.domain) return false
        if (code != other.code) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = domain.hashCode()
        result = 31 * result + code
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "PurchasesError(domain=$domain, code=$code, message=$message)"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(domain, flags)
        parcel.writeInt(code)
        parcel.writeString(message)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PurchasesError> {
        override fun createFromParcel(parcel: Parcel): PurchasesError {
            return PurchasesError(parcel)
        }

        override fun newArray(size: Int): Array<PurchasesError?> {
            return arrayOfNulls(size)
        }
    }
}
