package com.revenuecat.purchases.identity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class IdentityTests {

    // region identitySource per factory

    @Test
    fun `signInWithApple has identitySource SIGN_IN_WITH_APPLE`() {
        val identity = Identity.signInWithApple("token".toByteArray())
        assertThat(identity.identitySource).isEqualTo(IdentitySource.SIGN_IN_WITH_APPLE)
    }

    @Test
    fun `oidc has identitySource OIDC`() {
        val identity = Identity.oidc("token".toByteArray())
        assertThat(identity.identitySource).isEqualTo(IdentitySource.OIDC)
    }

    @Test
    fun `google has identitySource GOOGLE`() {
        val identity = Identity.google("token".toByteArray())
        assertThat(identity.identitySource).isEqualTo(IdentitySource.GOOGLE)
    }

    @Test
    fun `firebase has identitySource FIREBASE`() {
        val identity = Identity.firebase("token".toByteArray())
        assertThat(identity.identitySource).isEqualTo(IdentitySource.FIREBASE)
    }

    @Test
    fun `facebook has identitySource FACEBOOK`() {
        val identity = Identity.facebook("token".toByteArray(), email = "a@b.com")
        assertThat(identity.identitySource).isEqualTo(IdentitySource.FACEBOOK)
    }

    @Test
    fun `anonymous has identitySource ANONYMOUS`() {
        assertThat(Identity.anonymous.identitySource).isEqualTo(IdentitySource.ANONYMOUS)
    }

    // endregion

    // region equality

    @Test
    fun `identities from the same factory and payload are equal`() {
        val first = Identity.oidc("same-token".toByteArray())
        val second = Identity.oidc("same-token".toByteArray())
        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `identities from different payloads are not equal`() {
        val first = Identity.oidc("token-1".toByteArray())
        val second = Identity.oidc("token-2".toByteArray())
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `identities from different providers with identical payload bytes are not equal`() {
        val oidc = Identity.oidc("identical-bytes".toByteArray())
        val google = Identity.google("identical-bytes".toByteArray())
        assertThat(oidc).isNotEqualTo(google)
    }

    @Test
    fun `facebook identities with the same token but different emails are not equal`() {
        val withEmail = Identity.facebook("token".toByteArray(), email = "a@b.com")
        val withoutEmail = Identity.facebook("token".toByteArray(), email = null)
        assertThat(withEmail).isNotEqualTo(withoutEmail)
    }

    @Test
    fun `anonymous is a stable singleton-equal identity`() {
        assertThat(Identity.anonymous).isEqualTo(Identity.anonymous)
    }

    @Test
    fun `an identity is not equal to an unrelated type`() {
        val identity = Identity.oidc("token".toByteArray())
        assertThat(identity).isNotEqualTo("token")
    }

    // endregion
}
