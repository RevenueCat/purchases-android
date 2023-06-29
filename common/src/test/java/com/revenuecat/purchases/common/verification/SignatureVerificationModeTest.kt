package com.revenuecat.purchases.common.verification

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementVerificationMode
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
        val signatureVerifier = DefaultSignatureVerifier()
        assertThat(SignatureVerificationMode.Disabled.shouldVerify).isFalse
        assertThat(SignatureVerificationMode.Informational(signatureVerifier).shouldVerify).isTrue
        assertThat(SignatureVerificationMode.Enforced(signatureVerifier).shouldVerify).isTrue
    }

    @Test
    fun `intermediateSignatureHelper has values in enabled verification modes`() {
        val signatureVerifier = DefaultSignatureVerifier()
        var verificationMode: SignatureVerificationMode = SignatureVerificationMode.Disabled
        assertThat(verificationMode.intermediateSignatureHelper).isNull()
        verificationMode = SignatureVerificationMode.Informational(signatureVerifier)
        assertThat(verificationMode.intermediateSignatureHelper).isNotNull
        verificationMode = SignatureVerificationMode.Enforced(signatureVerifier)
        assertThat(verificationMode.intermediateSignatureHelper).isNotNull
    }
}
