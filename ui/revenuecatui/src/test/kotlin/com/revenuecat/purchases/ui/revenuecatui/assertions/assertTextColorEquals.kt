package com.revenuecat.purchases.ui.revenuecatui.assertions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteraction

/**
 * Assert this Composable's text color.
 */
internal fun SemanticsNodeInteraction.assertTextColorEquals(color: Color) =
    assertTextLayoutResult("Text has color '$color'") {
        it.layoutInput.style.color == color
    }
