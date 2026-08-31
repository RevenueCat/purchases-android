package com.revenuecat.purchases.common.remoteconfig

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigUtilsTest {

    @Test
    fun `isValidRef accepts a well-formed 32-char base64url ref`() {
        assertThat(RemoteConfigUtils.isValidRef("AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH")).isTrue()
        // The full URL-safe, unpadded alphabet (A-Z a-z 0-9 - _) at exactly 32 chars is valid.
        assertThat(RemoteConfigUtils.isValidRef("aB0_-cDeFgHiJkLmNoPqRsTuVwXyZ012")).isTrue()
    }

    @Test
    fun `isValidRef rejects malformed refs`() {
        assertThat(RemoteConfigUtils.isValidRef("")).isFalse()
        assertThat(RemoteConfigUtils.isValidRef("too-short")).isFalse()
        assertThat(RemoteConfigUtils.isValidRef("AAAABBBBCCCCDDDDEEEEFFFFGGGGHHH")).isFalse() // 31 chars
        assertThat(RemoteConfigUtils.isValidRef("AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHHI")).isFalse() // 33 chars
        // '+' and '/' and '=' are not part of the URL-safe, unpadded alphabet.
        assertThat(RemoteConfigUtils.isValidRef("AAAABBBBCCCCDDDDEEEEFFFFGGGGHHH+")).isFalse()
        assertThat(RemoteConfigUtils.isValidRef("AAAABBBBCCCCDDDDEEEEFFFFGGGGHH==")).isFalse()
    }

    @Test
    fun `contentAddressRef is the truncated SHA-256 as unpadded base64url`() {
        val bytes = "a workflow body".toByteArray()
        val expected = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(bytes).copyOf(REF_HASH_BYTES),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )

        val ref = RemoteConfigUtils.contentAddressRef(bytes)

        assertThat(ref).isEqualTo(expected)
        // A computed ref is always a valid ref shape, so it round-trips through the store guard.
        assertThat(RemoteConfigUtils.isValidRef(ref)).isTrue()
    }

    private companion object {
        private const val REF_HASH_BYTES = 24
    }
}
