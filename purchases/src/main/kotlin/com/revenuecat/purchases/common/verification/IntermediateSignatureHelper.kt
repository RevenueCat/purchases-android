package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.fromLittleEndianBytes
import com.revenuecat.purchases.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.days

internal class IntermediateSignatureHelper(
    rootSignatureVerifierProvider: () -> SignatureVerifier,
    warmUpScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    constructor(rootSignatureVerifier: SignatureVerifier) : this({ rootSignatureVerifier })

    /**
     * Created lazily, and warmed up in the background below, because creating it initializes Tink's Ed25519
     * constant table. That takes hundreds of milliseconds on Tink versions newer than the one we declare (an
     * app can get one transitively from another SDK), and this class is built while `Purchases.configure`
     * runs on the main thread. Null when Tink refuses to create it, which disables verification.
     */
    private val rootSignatureVerifier: Lazy<SignatureVerifier?> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            rootSignatureVerifierProvider()
        } catch (e: IllegalStateException) {
            // Tink throws when it is restricted to FIPS mode, since Ed25519 is not FIPS compatible. Fail open
            // instead of crashing: verification reports NOT_REQUESTED. The null is memoized, so the failed
            // attempt is never repeated.
            errorLog { "Error creating signature verifier: ${e.message}. Disabling signature verification." }
            null
        }
    }

    init {
        warmUpScope.launch { rootSignatureVerifier.value }
    }

    /**
     * Whether responses can be verified at all. Creates the root verifier if the warm up has not finished,
     * blocking the calling thread, so only call this off the main thread.
     */
    fun canVerify(): Boolean = rootSignatureVerifier.value != null

    fun createIntermediateKeyVerifierIfVerified(
        signature: Signature,
    ): Result<SignatureVerifier, PurchasesError> {
        val rootVerifier = rootSignatureVerifier.value ?: return Result.Error(
            PurchasesError(PurchasesErrorCode.SignatureVerificationError, "Signature verifier unavailable."),
        )
        val intermediateKeyMessageToVerify = signature.intermediateKeyExpiration + signature.intermediateKey
        return if (!rootVerifier.verify(signature.intermediateKeySignature, intermediateKeyMessageToVerify)) {
            Result.Error(
                PurchasesError(PurchasesErrorCode.SignatureVerificationError, "Error verifying intermediate key."),
            )
        } else {
            val intermediateKeyExpirationDate = getIntermediateKeyExpirationDate(signature.intermediateKeyExpiration)
            if (intermediateKeyExpirationDate.before(Date())) {
                Result.Error(
                    PurchasesError(
                        PurchasesErrorCode.SignatureVerificationError,
                        "Intermediate key expired at $intermediateKeyExpirationDate",
                    ),
                )
            } else {
                Result.Success(DefaultSignatureVerifier(signature.intermediateKey))
            }
        }
    }

    private fun getIntermediateKeyExpirationDate(intermediateKeyExpirationBytes: ByteArray): Date {
        val daysSince1970 = Int.fromLittleEndianBytes(intermediateKeyExpirationBytes).days
        return Date(daysSince1970.inWholeMilliseconds)
    }
}
