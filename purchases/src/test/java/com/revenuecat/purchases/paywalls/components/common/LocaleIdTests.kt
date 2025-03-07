package com.revenuecat.purchases.paywalls.components.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class LocaleIdTests(
    val locale: String,
    val args: Args,
) {

    data class Args(
        val expectedLanguage: String,
        val expectedRegion: String,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "en_US",
                Args(
                    expectedLanguage = "en",
                    expectedRegion = "US",
                ),
            ),
            arrayOf(
                "en-US",
                Args(
                    expectedLanguage = "en",
                    expectedRegion = "US",
                ),
            ),
            arrayOf(
                "en",
                Args(
                    expectedLanguage = "en",
                    expectedRegion = "",
                ),
            ),
            arrayOf(
                "",
                Args(
                    expectedLanguage = "",
                    expectedRegion = "",
                ),
            ),
        )
    }

    @Test
    fun `Should properly parse language and region`() {
        // Arrange, Act
        val actual = LocaleId(locale)

        // Assert
        assertThat(actual.language).isEqualTo(args.expectedLanguage)
        assertThat(actual.region).isEqualTo(args.expectedRegion)
    }

}
