package com.revenuecat.purchases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AsRedactedAPIKeyTest {

    @Test
    fun `asRedactedAPIKey behaves like iOS cases`() {
        assertThat("test_CtDegh822fag83yggTUVkajsJ".asRedactedAPIKey)
            .isEqualTo("test_Ct********ajsJ")

        // Exactly 6 characters after underscore (minimal redactable remainder)
        assertThat("test_123456".asRedactedAPIKey)
            .isEqualTo("test_12********3456")
        assertThat("api_abcdef".asRedactedAPIKey)
            .isEqualTo("api_ab********cdef")
        assertThat("_abcdef".asRedactedAPIKey)
            .isEqualTo("_ab********cdef")
        assertThat("a_123456".asRedactedAPIKey)
            .isEqualTo("a_12********3456")

        // Short remainder: <6 chars → should NOT redact
        assertThat("test_12345".asRedactedAPIKey).isEqualTo("test_12345")
        assertThat("_abc".asRedactedAPIKey).isEqualTo("_abc")
        assertThat("test_".asRedactedAPIKey).isEqualTo("test_")

        // No underscore at all → should NOT redact
        assertThat("noUnderscoreKey123456".asRedactedAPIKey)
            .isEqualTo("noUnderscoreKey123456")

        // Multiple underscores: only the first underscore counts
        assertThat("test_abcd_efghijkl".asRedactedAPIKey)
            .isEqualTo("test_ab********ijkl")

        // Empty string and single underscore → should NOT crash and should NOT redact
        assertThat("".asRedactedAPIKey).isEqualTo("")
        assertThat("_".asRedactedAPIKey).isEqualTo("_")
    }
}


