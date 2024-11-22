package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.common.OfferingParser
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DimensionTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    class Args(
        @Language("json")
        val json: String,
        val expected: Dimension,
    )

    companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "vertical",
                Args(
                    json = """
                        {
                          "alignment": "leading",
                          "distribution": "start",
                          "type": "vertical"
                        }      
                        """.trimIndent(),
                    expected = Dimension.Vertical(
                        alignment = HorizontalAlignment.LEADING,
                        distribution = FlexDistribution.START
                    )
                )
            ),
            arrayOf(
                "horizontal",
                Args(
                    json = """
                        {
                          "alignment": "bottom",
                          "distribution": "space_between",
                          "type": "horizontal"
                        }      
                        """.trimIndent(),
                    expected = Dimension.Horizontal(
                        alignment = VerticalAlignment.BOTTOM,
                        distribution = FlexDistribution.SPACE_BETWEEN
                    )
                )
            ),
            arrayOf(
                "zlayer",
                Args(
                    json = """
                        {
                          "alignment": "top_leading",
                          "type": "zlayer"
                        }      
                        """.trimIndent(),
                    expected = Dimension.ZLayer(
                        alignment = TwoDimensionalAlignment.TOP_LEADING,
                    )
                )
            ),
        )
    }

    @Test
    fun `Should properly deserialize Dimension`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<Dimension>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
