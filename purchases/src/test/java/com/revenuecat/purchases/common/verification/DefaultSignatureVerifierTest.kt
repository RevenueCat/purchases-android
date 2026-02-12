package com.revenuecat.purchases.common.verification

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DefaultSignatureVerifierTest {

    private val testPublicKey = "WEvg1j3R0lppwHRfLup/lzYZMQMEj5csDHeQe/YP1p4="

    private lateinit var verifier: DefaultSignatureVerifier

    @Before
    fun setUp() {
        verifier = DefaultSignatureVerifier(testPublicKey)
    }

    @Test
    fun `verify verifies correct message and signature`() {
        val signature = Base64.decode(
            "M8K3Bs4jsmiJXOovn7gQHMWs+9XxB9JQkxyAB7iEVo4z+vLOhkoZIyexckvMPJ9b\nUmgwIYkvl6ojWx/bbu2CBw==",
            Base64.DEFAULT
        )
        val message = "Test message".toByteArray()
        assertThat(verifier.verify(signature, message)).isTrue
    }

    @Test
    fun `verify verifies incorrect message`() {
        val signature = Base64.decode(
            "M8K3Bs4jsmiJXOovn7gQHMWs+9XxB9JQkxyAB7iEVo4z+vLOhkoZIyexckvMPJ9b\nUmgwIYkvl6ojWx/bbu2CBw==",
            Base64.DEFAULT
        )
        val message = "Test message2".toByteArray()
        assertThat(verifier.verify(signature, message)).isFalse
    }

    @Test
    fun `verify verifies incorrect signature`() {
        val signature = Base64.decode(
            "M8K3Bs4jsmiJXOovn7gQHMWs+9XxB9JQkxyAB7iEVo4z+vLOhkoZIyexckvMPJ9b\nUmgwIYkvl6ojWx/bbu3CBw==",
            Base64.DEFAULT
        )
        val message = "Test message".toByteArray()
        assertThat(verifier.verify(signature, message)).isFalse
    }
}
