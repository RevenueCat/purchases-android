package com.revenuecat.purchases.common.verification

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.utils.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.CoroutineContext

@RunWith(AndroidJUnit4::class)
class IntermediateSignatureHelperTest {

    private val validSignature = Signature.fromString("xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fsK4PUFrNz9mRS0v/InK81CwXmtbGoTy1bD8d+PValEX9dY+8zon/CM8Bx4oA2pUFgtHSaedPJfqnTjNPh0l0O62iWADAwrsih4z//uQoruUD3T5WXa2w7s7LFMnRFuRQY3uKz0StgC/qkPAufCtqzZqQZR1zDu9MxDzmG6eNAqcM3fsIV5sQIMmI3P0dEMDK5cM/YG")

    private lateinit var intermediateSignatureHelper: IntermediateSignatureHelper

    @Before
    fun setUp() {
        intermediateSignatureHelper = IntermediateSignatureHelper(
            DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")
        )
    }

    @Test
    fun `does not create the root verifier on the constructing thread`() {
        var createdRootVerifiers = 0
        val helper = IntermediateSignatureHelper(
            { createdRootVerifiers++; testRootVerifier() },
            CoroutineScope(NeverRunsDispatcher()),
        )
        assertThat(createdRootVerifiers).isZero

        assertThat(helper.canVerify()).isTrue
        assertThat(helper.canVerify()).isTrue
        assertThat(createdRootVerifiers).isEqualTo(1)
    }

    @Test
    fun `warms up the root verifier in the background`() {
        var createdRootVerifiers = 0
        IntermediateSignatureHelper(
            { createdRootVerifiers++; testRootVerifier() },
            CoroutineScope(Dispatchers.Unconfined),
        )

        assertThat(createdRootVerifiers).isEqualTo(1)
    }

    @Test
    fun `if the root verifier cannot be created, cannot verify and does not retry`() {
        var attemptedRootVerifiers = 0
        val helper = IntermediateSignatureHelper(
            { attemptedRootVerifiers++; throw IllegalStateException("Can not use Ed25519 in FIPS-mode.") },
            CoroutineScope(NeverRunsDispatcher()),
        )

        assertThat(helper.canVerify()).isFalse
        assertThat(helper.canVerify()).isFalse
        assertThat(attemptedRootVerifiers).isEqualTo(1)

        when (val result = helper.createIntermediateKeyVerifierIfVerified(validSignature)) {
            is Result.Success -> fail("Expected error")
            is Result.Error -> {
                assertThat(result.value.code).isEqualTo(PurchasesErrorCode.SignatureVerificationError)
                assertThat(result.value.underlyingErrorMessage).startsWith("Signature verifier unavailable.")
            }
        }
    }

    @Test
    fun `if fails to verify intermediate signature, returns error`() {
        val incorrectSignature = validSignature.copy(intermediateKey = "incorrect".toByteArray())
        when (val result = intermediateSignatureHelper.createIntermediateKeyVerifierIfVerified(incorrectSignature)) {
            is Result.Success -> fail("Expected error")
            is Result.Error -> {
                assertThat(result.value.code).isEqualTo(PurchasesErrorCode.SignatureVerificationError)
                assertThat(result.value.underlyingErrorMessage).startsWith("Error verifying intermediate key.")
            }
        }
    }

    @Test
    fun `if intermediate signature is expired, returns error`() {
        val expiredIntermediateKeySignature = Signature.fromString("xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fsKAAAAJMOXT1iHMIJlcZ0KNSvIHu+PE5DfETod7ix/ggbABphqbzt7t8p+ZwFpqc7K+1n6lzcsWAyKCWU7ofXoOLF8D0Cfn6wrs56pEGFLZxBvv9m46nIAJz8zmn+LmHo6kRellbweWMo8fbrb08mReRxdqB++3GyQWyHbvOS7yQW/od193UracSQNMH+4wXbcjCwG")
        when (val result = intermediateSignatureHelper.createIntermediateKeyVerifierIfVerified(expiredIntermediateKeySignature)) {
            is Result.Success -> fail("Expected error")
            is Result.Error -> {
                assertThat(result.value.code).isEqualTo(PurchasesErrorCode.SignatureVerificationError)
                assertThat(result.value.underlyingErrorMessage).startsWith("Intermediate key expired")
            }
        }
    }

    @Test
    fun `if intermediate signature is valid, returns verifier`() {
        when (val result = intermediateSignatureHelper.createIntermediateKeyVerifierIfVerified(validSignature)) {
            is Result.Success -> {
                assertThat(result.value).isInstanceOf(DefaultSignatureVerifier::class.java)
            }
            is Result.Error -> fail("Expected success")
        }
    }

    private fun testRootVerifier() = DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")

    // Keeps the background warm up from running, so that tests can tell apart what the constructor does from
    // what the first canVerify call does.
    private class NeverRunsDispatcher : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) = Unit
    }
}
