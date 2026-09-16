package com.revenuecat.purchases.identity

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class IdentitySourceTests {

    // region rawValue round trip

    @Test
    fun `fromRawValue returns ANONYMOUS for anonymous`() {
        assertThat(IdentitySource.fromRawValue("anonymous")).isEqualTo(IdentitySource.ANONYMOUS)
    }

    @Test
    fun `fromRawValue returns OIDC for oidc`() {
        assertThat(IdentitySource.fromRawValue("oidc")).isEqualTo(IdentitySource.OIDC)
    }

    @Test
    fun `fromRawValue returns GOOGLE for google`() {
        assertThat(IdentitySource.fromRawValue("google")).isEqualTo(IdentitySource.GOOGLE)
    }

    @Test
    fun `fromRawValue returns SIGN_IN_WITH_APPLE for apple`() {
        assertThat(IdentitySource.fromRawValue("apple")).isEqualTo(IdentitySource.SIGN_IN_WITH_APPLE)
    }

    @Test
    fun `fromRawValue returns FACEBOOK for facebook`() {
        assertThat(IdentitySource.fromRawValue("facebook")).isEqualTo(IdentitySource.FACEBOOK)
    }

    @Test
    fun `fromRawValue returns FIREBASE for firebase`() {
        assertThat(IdentitySource.fromRawValue("firebase")).isEqualTo(IdentitySource.FIREBASE)
    }

    @Test
    fun `every case round trips through its own rawValue`() {
        IdentitySource.values().forEach { source ->
            assertThat(IdentitySource.fromRawValue(source.rawValue)).isEqualTo(source)
        }
    }

    // endregion

    // region unknown values

    @Test
    fun `fromRawValue returns null for an unrecognized raw value`() {
        assertThat(IdentitySource.fromRawValue("myspace")).isNull()
    }

    @Test
    fun `fromRawValue returns null for an empty string`() {
        assertThat(IdentitySource.fromRawValue("")).isNull()
    }

    @Test
    fun `fromRawValue is case-sensitive`() {
        assertThat(IdentitySource.fromRawValue("Google")).isNull()
        assertThat(IdentitySource.fromRawValue("GOOGLE")).isNull()
    }

    // endregion
}
