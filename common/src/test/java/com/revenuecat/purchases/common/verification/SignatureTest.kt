package com.revenuecat.purchases.common.verification

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignatureTest {

    private val signature = Signature.fromString("nVoKJjLhhTNo19Mkjr5DEmgMf361HWxxMyctC10Ob7f/////+GStaG6mLGXfe+T+p6jDqBkuLHfF3VaCOYLwpCfWQBzeTGXB7ntSs4ESiw9sxHy0VTR0P5mSDxkSteR/qAANCFfQSkHeWl4NJ4IDusH1iehUgiku0dMOx5+u53eU3eB45bV7Uttc/AX9bSzpwinw1hqRpuNOyNZOQk0r+vDokRcMlC9XgraztIAO+m0LLtMF")

    @Test
    fun `components have correct ranges`() {
        assertThat(SignatureComponent.INTERMEDIATE_KEY.startByte).isEqualTo(0)
        assertThat(SignatureComponent.INTERMEDIATE_KEY.endByte).isEqualTo(32)
        assertThat(SignatureComponent.INTERMEDIATE_KEY_EXPIRATION.startByte).isEqualTo(32)
        assertThat(SignatureComponent.INTERMEDIATE_KEY_EXPIRATION.endByte).isEqualTo(36)
        assertThat(SignatureComponent.INTERMEDIATE_KEY_SIGNATURE.startByte).isEqualTo(36)
        assertThat(SignatureComponent.INTERMEDIATE_KEY_SIGNATURE.endByte).isEqualTo(100)
        assertThat(SignatureComponent.SALT.startByte).isEqualTo(100)
        assertThat(SignatureComponent.SALT.endByte).isEqualTo(116)
        assertThat(SignatureComponent.PAYLOAD.startByte).isEqualTo(116)
        assertThat(SignatureComponent.PAYLOAD.endByte).isEqualTo(180)
    }

    @Test
    fun `fromString parses correctly`() {
        assertThat(signature.intermediateKey.size).isEqualTo(SignatureComponent.INTERMEDIATE_KEY.size)
        assertThat(signature.intermediateKeyExpiration.size).isEqualTo(SignatureComponent.INTERMEDIATE_KEY_EXPIRATION.size)
        assertThat(signature.intermediateKeySignature.size).isEqualTo(SignatureComponent.INTERMEDIATE_KEY_SIGNATURE.size)
        assertThat(signature.salt.size).isEqualTo(SignatureComponent.SALT.size)
        assertThat(signature.payload.size).isEqualTo(SignatureComponent.PAYLOAD.size)
    }

    @Test
    fun `fromString errors if invalid signature size`() {
        assertThatExceptionOfType(InvalidSignatureSizeException::class.java).isThrownBy {
            Signature.fromString("nVoKJjLhhTNo19M")
        }
    }

    @Test
    fun `equals checks all fields`() {
        val otherSignature = Signature(
            intermediateKey = signature.intermediateKey.copyOf(),
            intermediateKeyExpiration = signature.intermediateKeyExpiration.copyOf(),
            intermediateKeySignature = signature.intermediateKeySignature.copyOf(),
            salt = signature.salt.copyOf(),
            payload = signature.payload.copyOf(),
        )
        assertThat(signature).isEqualTo(otherSignature)
    }

    @Test
    fun `equals is false if small changes`() {
        val otherSignature = Signature(
            intermediateKey = signature.intermediateKey.copyOfRange(0, 1),
            intermediateKeyExpiration = signature.intermediateKeyExpiration.copyOf(),
            intermediateKeySignature = signature.intermediateKeySignature.copyOf(),
            salt = signature.salt.copyOf(),
            payload = signature.payload.copyOf(),
        )
        assertThat(signature).isNotEqualTo(otherSignature)
    }

    @Test
    fun `hashCode includes all fields`() {
        val otherSignature = Signature(
            intermediateKey = signature.intermediateKey.copyOf(),
            intermediateKeyExpiration = signature.intermediateKeyExpiration.copyOf(),
            intermediateKeySignature = signature.intermediateKeySignature.copyOf(),
            salt = signature.salt.copyOf(),
            payload = signature.payload.copyOf(),
        )
        assertThat(signature.hashCode()).isEqualTo(otherSignature.hashCode())
    }

    @Test
    fun `hashCode changes if different`() {
        val otherSignature = Signature(
            intermediateKey = signature.intermediateKey.copyOfRange(0, 1),
            intermediateKeyExpiration = signature.intermediateKeyExpiration.copyOf(),
            intermediateKeySignature = signature.intermediateKeySignature.copyOf(),
            salt = signature.salt.copyOf(),
            payload = signature.payload.copyOf(),
        )
        assertThat(signature.hashCode()).isNotEqualTo(otherSignature.hashCode())
    }
}
