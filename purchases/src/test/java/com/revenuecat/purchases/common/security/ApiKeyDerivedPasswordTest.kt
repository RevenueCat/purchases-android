package com.revenuecat.purchases.common.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.assertErrorLog
import org.assertj.core.api.Assertions.assertThat
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
        assertThat(first).isNotNull()
        assertThat(second).isNotNull()
        assertThat(first!!).isEqualTo(second!!)
    }

    @Test
    fun `different API keys derive different passwords`() {
        val first = derivePassword("test_api_key_12345")
        val second = derivePassword("test_api_key_67890")
        assertThat(first).isNotNull()
        assertThat(second).isNotNull()
        assertThat(first!!).isNotEqualTo(second!!)
    }

    @Test
    fun `derived password reflects the API key contents`() {
        val apiKey = "test_api_key_12345"
        val password = derivePassword(apiKey)
        assertThat(password).isNotNull()
        assertThat(password!!).isEqualTo(apiKey.toCharArray())
    }

    // endregion

    // region blank/empty API key

    @Test
    fun `blank API key returns null instead of throwing`() {
        assertThat(derivePassword("   ")).isNull()
    }

    @Test
    fun `empty API key returns null instead of throwing`() {
        assertThat(derivePassword("")).isNull()
    }

    @Test
    fun `blank API key logs an error instead of crashing`() {
        assertErrorLog(
            "Cannot derive an IAM storage password from a blank API key. IAM login will be unavailable.",
        ) {
            derivePassword("   ")
        }
    }

    @Test
    fun `empty API key logs an error instead of crashing`() {
        assertErrorLog(
            "Cannot derive an IAM storage password from a blank API key. IAM login will be unavailable.",
        ) {
            derivePassword("")
        }
    }

    // endregion
}
