package com.revenuecat.purchases.common.verification

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementVerificationMode
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SignatureVerificationModeTest {

    @Test
    fun `fromEntitlementVerificationMode transforms verification mode correctly`() {
        assertThat(
            SignatureVerificationMode.fromEntitlementVerificationMode(EntitlementVerificationMode.DISABLED)
        ).isEqualTo(SignatureVerificationMode.Disabled)
        assertThat(
            SignatureVerificationMode.fromEntitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
        ).isInstanceOf(SignatureVerificationMode.Informational::class.java)
        // Hidden ENFORCED mode during feature beta
//         assertThat(
//             SignatureVerificationMode.fromEntitlementVerificationMode(EntitlementVerificationMode.ENFORCED)
//         ).isInstanceOf(SignatureVerificationMode.Enforced::class.java)
    }

    @Test
    fun `shouldVerify has correct values for all the verification modes`() {
        assertThat(SignatureVerificationMode.Disabled.shouldVerify).isFalse
        assertThat(SignatureVerificationMode.Informational().shouldVerify).isTrue
        assertThat(SignatureVerificationMode.Enforced().shouldVerify).isTrue
    }

    @Test
    fun `fromEntitlementVerificationMode does not create the root verifier`() {
        var createdRootVerifiers = 0
        val verificationMode = SignatureVerificationMode.fromEntitlementVerificationMode(
            EntitlementVerificationMode.INFORMATIONAL,
        ) {
            createdRootVerifiers++
            mockk()
        }
        assertThat(createdRootVerifiers).isZero
        assertThat(verificationMode.shouldVerify).isTrue
        assertThat(createdRootVerifiers).isZero

        assertThat(verificationMode.intermediateSignatureHelper).isNotNull
        assertThat(verificationMode.intermediateSignatureHelper).isNotNull
        assertThat(createdRootVerifiers).isEqualTo(1)
    }

    @Test
    fun `intermediateSignatureHelper is null if the root verifier cannot be created`() {
        var attemptedRootVerifiers = 0
        val verificationMode = SignatureVerificationMode.fromEntitlementVerificationMode(
            EntitlementVerificationMode.INFORMATIONAL,
        ) {
            attemptedRootVerifiers++
            throw IllegalStateException("Can not use Ed25519 in FIPS-mode.")
        }

        assertThat(verificationMode.intermediateSignatureHelper).isNull()
        assertThat(verificationMode.intermediateSignatureHelper).isNull()
        assertThat(attemptedRootVerifiers).isEqualTo(1)
    }

    @Test
    fun `warmUp returns whether signature verification is available`() {
        assertThat(
            SignatureVerificationMode.fromEntitlementVerificationMode(
                EntitlementVerificationMode.INFORMATIONAL,
            ) { mockk() }.warmUp()
        ).isTrue
        assertThat(
            SignatureVerificationMode.fromEntitlementVerificationMode(
                EntitlementVerificationMode.INFORMATIONAL,
            ) { throw IllegalStateException("Can not use Ed25519 in FIPS-mode.") }.warmUp()
        ).isFalse
        assertThat(SignatureVerificationMode.Disabled.warmUp()).isFalse
    }

    @Test
    fun `intermediateSignatureHelper has values in enabled verification modes`() {
        var verificationMode: SignatureVerificationMode = SignatureVerificationMode.Disabled
        assertThat(verificationMode.intermediateSignatureHelper).isNull()
        verificationMode = SignatureVerificationMode.Informational()
        assertThat(verificationMode.intermediateSignatureHelper).isNotNull
        verificationMode = SignatureVerificationMode.Enforced()
        assertThat(verificationMode.intermediateSignatureHelper).isNotNull
    }
}
