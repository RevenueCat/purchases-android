package com.revenuecat.purchases.common.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ApiKeyDerivedPasswordTest {

    // region determinism / uniqueness

    @Test
    fun `same API key derives the same password`() {
        val first = derivePassword("test_api_key_12345")
        val second = derivePassword("test_api_key_12345")
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `different API keys derive different passwords`() {
        val first = derivePassword("test_api_key_12345")
        val second = derivePassword("test_api_key_67890")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `derived password reflects the API key contents`() {
        val apiKey = "test_api_key_12345"
        val password = derivePassword(apiKey)
        assertThat(password).isEqualTo(apiKey.toCharArray())
    }

    // endregion

    // region blank/empty rejection

    @Test
    fun `blank API key is rejected`() {
        assertThatThrownBy { derivePassword("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `empty API key is rejected`() {
        assertThatThrownBy { derivePassword("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejection error message is clear`() {
        assertThatThrownBy { derivePassword("") }
            .hasMessageContaining("blank API key")
    }

    // endregion
}
