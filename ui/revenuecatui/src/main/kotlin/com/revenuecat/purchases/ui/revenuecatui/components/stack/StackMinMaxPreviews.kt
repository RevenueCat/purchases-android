@file:JvmSynthetic
@file:Suppress("MagicNumber")

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle

private val previewBackground = Color(0xFFE2E8F0)
private val containerBackground = BackgroundStyles.Color(ColorStyles(ColorStyle.Solid(Color(0xFFCBD5E1))))
private val childColors = listOf(
    ColorStyles(ColorStyle.Solid(Color(0xFFDC2626))),
    ColorStyles(ColorStyle.Solid(Color(0xFF2563EB))),
    ColorStyles(ColorStyle.Solid(Color(0xFF16A34A))),
)

@Preview(name = "Size · Fill constraints", widthDp = 320, heightDp = 430, showBackground = true)
@Composable
private fun FillConstraintsPreview() {
    ConstraintGallery(
        cases = listOf(
            "Fill()" to Fill(),
            "Fill(min = 180)" to Fill(min = 180u),
            "Fill(max = 120)" to Fill(max = 120u),
            "Fill(min = 100, max = 180)" to Fill(min = 100u, max = 180u),
        ),
    )
}

@Preview(name = "Size · Fit constraints", widthDp = 320, heightDp = 430, showBackground = true)
@Composable
private fun FitConstraintsPreview() {
    ConstraintGallery(
        cases = listOf(
            "Fit()" to Fit(),
            "Fit(min = 180)" to Fit(min = 180u),
            "Fit(max = 120)" to Fit(max = 120u),
            "Fit(min = 100, max = 180)" to Fit(min = 100u, max = 180u),
        ),
    )
}

@Preview(name = "Margin · Fill maximum", widthDp = 320, heightDp = 140, showBackground = true)
@Composable
private fun FillMaximumWithMarginPreview() {
    MarginConstraintPreview(
        title = "Fill(max=32) + 16dp margins",
        constraint = Fill(max = 32u),
    )
}

@Preview(name = "Margin · Fit maximum", widthDp = 320, heightDp = 140, showBackground = true)
@Composable
private fun FitMaximumWithMarginPreview() {
    MarginConstraintPreview(
        title = "Fit(max=32) + 16dp margins",
        constraint = Fit(max = 32u),
    )
}

@Preview(name = "Stack · Horizontal equal Fill", widthDp = 320, heightDp = 150, showBackground = true)
@Composable
private fun HorizontalEqualFillPreview() {
    StackPreview(
        title = "240dp: Fill() + Fill()",
        horizontal = true,
        constraints = listOf(Fill(), Fill()),
    )
}

@Preview(name = "Stack · Horizontal minimum", widthDp = 320, heightDp = 150, showBackground = true)
@Composable
private fun HorizontalMinimumPreview() {
    StackPreview(
        title = "240dp: Fill(min=180) + Fill()",
        horizontal = true,
        constraints = listOf(Fill(min = 180u), Fill()),
    )
}

@Preview(name = "Stack · Horizontal maximum", widthDp = 320, heightDp = 150, showBackground = true)
@Composable
private fun HorizontalMaximumPreview() {
    StackPreview(
        title = "240dp: Fill(max=60) + Fill()",
        horizontal = true,
        constraints = listOf(Fill(max = 60u), Fill()),
    )
}

@Preview(name = "Stack · Horizontal mixed and overflow", widthDp = 360, heightDp = 300, showBackground = true)
@Composable
private fun HorizontalMixedBoundsPreview() {
    Column {
        StackPreview(
            title = "240dp: min=140, max=40, Fill()",
            horizontal = true,
            constraints = listOf(Fill(min = 140u), Fill(max = 40u), Fill()),
        )
        StackPreview(
            title = "240dp: min=160 + min=160 (overflow)",
            horizontal = true,
            constraints = listOf(Fill(min = 160u), Fill(min = 160u)),
        )
    }
}

@Preview(name = "Stack · Vertical minimum", widthDp = 320, heightDp = 330, showBackground = true)
@Composable
private fun VerticalMinimumPreview() {
    StackPreview(
        title = "240dp: Fill(min=180) + Fill()",
        horizontal = false,
        constraints = listOf(Fill(min = 180u), Fill()),
    )
}

@Preview(name = "Stack · Vertical maximum", widthDp = 320, heightDp = 330, showBackground = true)
@Composable
private fun VerticalMaximumPreview() {
    StackPreview(
        title = "240dp: Fill(max=60) + Fill()",
        horizontal = false,
        constraints = listOf(Fill(max = 60u), Fill()),
    )
}

@Composable
private fun ConstraintGallery(cases: List<Pair<String, SizeConstraint>>) {
    Column(
        modifier = Modifier
            .background(previewBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cases.forEach { (label, constraint) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = label)
                Box(
                    modifier = Modifier
                        .requiredWidth(280.dp)
                        .requiredHeight(64.dp)
                        .background(Color(0xFFCBD5E1)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2563EB))
                            .size(Size(width = constraint, height = Fixed(40u)))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "Intrinsic content wider than 120dp",
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarginConstraintPreview(
    title: String,
    constraint: SizeConstraint,
) {
    Column(
        modifier = Modifier
            .background(previewBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title)
        Row(
            modifier = Modifier.background(Color(0xFFCBD5E1)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StackComponentView(
                style = previewStackComponentStyle(
                    children = listOf(
                        previewTextComponentStyle(
                            text = "32",
                            color = ColorStyles(ColorStyle.Solid(Color.White)),
                            size = Size(width = Fixed(100u), height = Fixed(32u)),
                        ),
                    ),
                    size = Size(width = constraint, height = Fixed(32u)),
                    spacing = 0.dp,
                    background = BackgroundStyles.Color(childColors[1]),
                    padding = PaddingValues(0.dp),
                    margin = PaddingValues(horizontal = 16.dp),
                    shape = Shape.Rectangle(),
                    border = null,
                ),
                state = previewEmptyState(),
                clickHandler = {},
            )
            Box(
                modifier = Modifier
                    .requiredWidth(4.dp)
                    .requiredHeight(32.dp)
                    .background(Color.Magenta),
            )
        }
        Text(text = "32dp content + 32dp margin; marker starts at 64dp")
    }
}

@Composable
private fun StackPreview(
    title: String,
    horizontal: Boolean,
    constraints: List<Fill>,
) {
    val children = constraints.mapIndexed { index, constraint ->
        previewChild(
            label = when {
                constraint.min != null -> "min ${constraint.min}"
                constraint.max != null -> "max ${constraint.max}"
                else -> "Fill"
            },
            color = childColors[index % childColors.size],
            constraint = constraint,
            horizontal = horizontal,
        )
    }
    val dimension = if (horizontal) {
        Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START)
    } else {
        Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START)
    }
    val size = if (horizontal) {
        Size(width = Fixed(240u), height = Fixed(72u))
    } else {
        Size(width = Fixed(240u), height = Fixed(240u))
    }

    Column(
        modifier = Modifier
            .background(previewBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title)
        Box(
            modifier = Modifier
                .size(size)
                .border(
                    width = 1.dp,
                    color = Color.Magenta,
                ),
        ) {
            StackComponentView(
                style = previewStackComponentStyle(
                    children = children,
                    dimension = dimension,
                    size = size,
                    spacing = 0.dp,
                    background = containerBackground,
                    padding = PaddingValues(0.dp),
                    border = null,
                ),
                state = previewEmptyState(),
                clickHandler = {},
            )
        }
    }
}

private fun previewChild(
    label: String,
    color: ColorStyles,
    constraint: Fill,
    horizontal: Boolean,
): ComponentStyle = previewTextComponentStyle(
    text = label,
    color = ColorStyles(ColorStyle.Solid(Color.White)),
    backgroundColor = color,
    size = if (horizontal) {
        Size(width = constraint, height = Fill())
    } else {
        Size(width = Fill(), height = constraint)
    },
    padding = Padding(top = 8.0, bottom = 8.0, leading = 4.0, trailing = 4.0),
)
