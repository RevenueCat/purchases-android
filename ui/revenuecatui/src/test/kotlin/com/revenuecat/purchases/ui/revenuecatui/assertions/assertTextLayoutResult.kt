package com.revenuecat.purchases.ui.revenuecatui.assertions

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.TextLayoutResult

/**
 * Assert any aspect of this Composable's [TextLayoutResult]. This is typically used for Text Composables.
 */
internal fun SemanticsNodeInteraction.assertTextLayoutResult(
    description: String,
    predicate: (TextLayoutResult) -> Boolean,
) = assert(
    SemanticsMatcher(description) { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)

        if (results.isEmpty()) false
        else predicate(results.first())
    }
)
