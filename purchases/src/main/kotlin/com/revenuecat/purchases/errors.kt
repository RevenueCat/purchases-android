package com.revenuecat.purchases

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

public typealias PurchasesErrorCallback = (PurchasesError) -> Unit

/**
 * This class represents an error
 * @param code Error code
 * @param underlyingErrorMessage Optional error message with a more detailed explanation of the
 * error that originated this.
 */
@Parcelize
@Immutable
public class PurchasesError(
    public val code: PurchasesErrorCode,
    public val underlyingErrorMessage: String? = null,
) : Parcelable, Serializable {

    public companion object {
        private const val serialVersionUID = 81719171L
    }

    // Message explaining the error
    public val message: String
        get() = code.description

    override fun toString(): String {
        return "PurchasesError(code=$code, underlyingErrorMessage=$underlyingErrorMessage, message='$message')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchasesError

        if (code != other.code) return false
        if (underlyingErrorMessage != other.underlyingErrorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + (underlyingErrorMessage?.hashCode() ?: 0)
        return result
    }
}
