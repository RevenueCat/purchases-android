package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.fromLittleEndianBytes
import com.revenuecat.purchases.utils.Result
import java.util.Date
import kotlin.time.Duration.Companion.days

internal class IntermediateSignatureHelper(
    private val rootSignatureVerifier: SignatureVerifier,
) {

    @Suppress("ReturnCount")
    fun createIntermediateKeyVerifierIfVerified(
        signature: Signature,
    ): Result<SignatureVerifier, PurchasesError> {
        val intermediateKeyMessageToVerify = signature.intermediateKeyExpiration + signature.intermediateKey
        if (!rootSignatureVerifier.verify(signature.intermediateKeySignature, intermediateKeyMessageToVerify)) {
            return Result.Error(
                PurchasesError(PurchasesErrorCode.SignatureVerificationError, "Error verifying intermediate key."),
            )
        }
        val intermediateKeyExpirationDate = getIntermediateKeyExpirationDate(signature.intermediateKeyExpiration)
        if (intermediateKeyExpirationDate.before(Date())) {
            return Result.Error(
                PurchasesError(
                    PurchasesErrorCode.SignatureVerificationError,
                    "Intermediate key expired at $intermediateKeyExpirationDate",
                ),
            )
        }
        return Result.Success(DefaultSignatureVerifier(signature.intermediateKey))
    }

    private fun getIntermediateKeyExpirationDate(intermediateKeyExpirationBytes: ByteArray): Date {
        val daysSince1970 = Int.fromLittleEndianBytes(intermediateKeyExpirationBytes).days
        return Date(daysSince1970.inWholeMilliseconds)
    }
}
