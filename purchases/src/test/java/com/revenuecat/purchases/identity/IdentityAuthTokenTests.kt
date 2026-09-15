package com.revenuecat.purchases.identity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class IdentityAuthTokenTests {

    // region authenticationMethod

    @Test
    fun `Anonymous authenticationMethod is ANONYMOUS`() {
        assertThat(IdentityAuthToken.Anonymous.authenticationMethod).isEqualTo(IdentitySource.ANONYMOUS)
    }

    @Test
    fun `Oidc authenticationMethod is OIDC`() {
        val token = IdentityAuthToken.Oidc("token".toByteArray())
        assertThat(token.authenticationMethod).isEqualTo(IdentitySource.OIDC)
    }

    @Test
    fun `Google authenticationMethod is GOOGLE`() {
        val token = IdentityAuthToken.Google("token".toByteArray())
        assertThat(token.authenticationMethod).isEqualTo(IdentitySource.GOOGLE)
    }

    @Test
    fun `SignInWithApple authenticationMethod is SIGN_IN_WITH_APPLE`() {
        val token = IdentityAuthToken.SignInWithApple("token".toByteArray())
        assertThat(token.authenticationMethod).isEqualTo(IdentitySource.SIGN_IN_WITH_APPLE)
    }

    @Test
    fun `Facebook authenticationMethod is FACEBOOK`() {
        val token = IdentityAuthToken.Facebook("token".toByteArray(), email = "a@b.com")
        assertThat(token.authenticationMethod).isEqualTo(IdentitySource.FACEBOOK)
    }

    @Test
    fun `Firebase authenticationMethod is FIREBASE`() {
        val token = IdentityAuthToken.Firebase("token".toByteArray())
        assertThat(token.authenticationMethod).isEqualTo(IdentitySource.FIREBASE)
    }

    // endregion

    // region validate()

    @Test
    fun `Anonymous always validates`() {
        assertThat(IdentityAuthToken.Anonymous.validate()).isTrue()
    }

    @Test
    fun `Oidc validates only with a non-empty payload`() {
        assertThat(IdentityAuthToken.Oidc(ByteArray(0)).validate()).isFalse()
        assertThat(IdentityAuthToken.Oidc("token".toByteArray()).validate()).isTrue()
    }

    @Test
    fun `Google validates only with a non-empty payload`() {
        assertThat(IdentityAuthToken.Google(ByteArray(0)).validate()).isFalse()
        assertThat(IdentityAuthToken.Google("token".toByteArray()).validate()).isTrue()
    }

    @Test
    fun `SignInWithApple validates only with a non-empty payload`() {
        assertThat(IdentityAuthToken.SignInWithApple(ByteArray(0)).validate()).isFalse()
        assertThat(IdentityAuthToken.SignInWithApple("token".toByteArray()).validate()).isTrue()
    }

    @Test
    fun `Facebook validates only with a non-empty payload, regardless of email`() {
        assertThat(IdentityAuthToken.Facebook(ByteArray(0), email = null).validate()).isFalse()
        assertThat(IdentityAuthToken.Facebook(ByteArray(0), email = "a@b.com").validate()).isFalse()
        assertThat(IdentityAuthToken.Facebook("token".toByteArray(), email = null).validate()).isTrue()
        assertThat(IdentityAuthToken.Facebook("token".toByteArray(), email = "a@b.com").validate()).isTrue()
    }

    @Test
    fun `Firebase validates only with a non-empty payload`() {
        assertThat(IdentityAuthToken.Firebase(ByteArray(0)).validate()).isFalse()
        assertThat(IdentityAuthToken.Firebase("token".toByteArray()).validate()).isTrue()
    }

    // endregion

    // region cacheIdentifier

    @Test
    fun `cacheIdentifier is stable for the same payload`() {
        val first = IdentityAuthToken.Oidc("same-token".toByteArray())
        val second = IdentityAuthToken.Oidc("same-token".toByteArray())
        assertThat(first.cacheIdentifier).isEqualTo(second.cacheIdentifier)
    }

    @Test
    fun `cacheIdentifier is stable for Anonymous`() {
        assertThat(IdentityAuthToken.Anonymous.cacheIdentifier).isEqualTo(IdentityAuthToken.Anonymous.cacheIdentifier)
    }

    @Test
    fun `cacheIdentifier differs for different payloads within the same case`() {
        val first = IdentityAuthToken.Google("token-1".toByteArray())
        val second = IdentityAuthToken.Google("token-2".toByteArray())
        assertThat(first.cacheIdentifier).isNotEqualTo(second.cacheIdentifier)
    }

    @Test
    fun `cacheIdentifier differs across authentication methods for identical payload bytes`() {
        val oidc = IdentityAuthToken.Oidc("identical-bytes".toByteArray())
        val google = IdentityAuthToken.Google("identical-bytes".toByteArray())
        val apple = IdentityAuthToken.SignInWithApple("identical-bytes".toByteArray())
        val firebase = IdentityAuthToken.Firebase("identical-bytes".toByteArray())

        val identifiers = listOf(oidc, google, apple, firebase).map { it.cacheIdentifier }
        assertThat(identifiers).doesNotHaveDuplicates()
    }

    @Test
    fun `Facebook cacheIdentifier differs by email even with the same identity token`() {
        val withoutEmail = IdentityAuthToken.Facebook("token".toByteArray(), email = null)
        val withEmail = IdentityAuthToken.Facebook("token".toByteArray(), email = "a@b.com")
        val withDifferentEmail = IdentityAuthToken.Facebook("token".toByteArray(), email = "c@d.com")

        val identifiers = listOf(withoutEmail, withEmail, withDifferentEmail).map { it.cacheIdentifier }
        assertThat(identifiers).doesNotHaveDuplicates()
    }

    @Test
    fun `Anonymous cacheIdentifier differs from every other case, even with an empty payload`() {
        val identifiers = listOf(
            IdentityAuthToken.Anonymous.cacheIdentifier,
            IdentityAuthToken.Oidc(ByteArray(0)).cacheIdentifier,
            IdentityAuthToken.Google(ByteArray(0)).cacheIdentifier,
            IdentityAuthToken.SignInWithApple(ByteArray(0)).cacheIdentifier,
            IdentityAuthToken.Firebase(ByteArray(0)).cacheIdentifier,
            IdentityAuthToken.Facebook(ByteArray(0), email = null).cacheIdentifier,
        )
        assertThat(identifiers).doesNotHaveDuplicates()
    }

    // endregion
}
