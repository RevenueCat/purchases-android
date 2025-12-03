package com.revenuecat.purchases.ui.revenuecatui.helpers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ToVersionIntTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val input: String,
    private val expected: Int?,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<Array<Any?>> = listOf(
            arrayOf("simple version string", "1.0.0", 100),
            arrayOf("version with single digits", "3.2.1", 321),
            arrayOf("version with larger numbers", "12.34.56", 123456),
            arrayOf("version with beta suffix", "1.0.0-beta1", 1001),
            arrayOf("version with alpha suffix", "2.5.3-alpha", 253),
            arrayOf("version with rc suffix", "1.2.3-rc1", 1231),
            arrayOf("version with build number", "1.0.0+build123", 100123),
            arrayOf("empty string", "", null),
            arrayOf("only dots", "...", null),
            arrayOf("only letters", "abc", null),
            arrayOf("single digit", "1", 1),
            arrayOf("two part version", "1.2", 12),
            arrayOf("version with leading zeros", "01.02.03", 10203),
            arrayOf("version with spaces", "1 . 2 . 3", 123),
            arrayOf("version with v prefix", "v1.2.3", 123),
        )
    }

    @Test
    fun `toVersionInt converts version string to integer correctly`() {
        val actual = input.toVersionInt()
        assertThat(actual).isEqualTo(expected)
    }
}
