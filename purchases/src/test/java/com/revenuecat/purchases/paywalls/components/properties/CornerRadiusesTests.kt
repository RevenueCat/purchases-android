package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class CornerRadiusesTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: CornerRadiuses,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "ints",
                Args(
                    json = """
                        {
                          "top_leading": 1,
                          "top_trailing": 2,
                          "bottom_leading": 3,
                          "bottom_trailing": 4
                        }
                        """.trimIndent(),
                    expected = CornerRadiuses(
                        topLeading = 1.0,
                        topTrailing = 2.0,
                        bottomLeading = 3.0,
                        bottomTrailing = 4.0,
                    )
                )
            ),
            arrayOf(
                "floats",
                Args(
                    json = """
                        {
                          "top_leading": 1.2,
                          "top_trailing": 2.3,
                          "bottom_leading": 3.4,
                          "bottom_trailing": 4.5
                        }
                        """.trimIndent(),
                    expected = CornerRadiuses(
                        topLeading = 1.2,
                        topTrailing = 2.3,
                        bottomLeading = 3.4,
                        bottomTrailing = 4.5,
                    )
                )
            ),
        )
    }

    @Test
    fun `Should properly deserialize CornerRadiuses`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<CornerRadiuses>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
