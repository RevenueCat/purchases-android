package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.composables.Fade
import com.revenuecat.purchases.ui.revenuecatui.composables.PlaceholderDefaults
import com.revenuecat.purchases.ui.revenuecatui.composables.placeholder

/**
 * True while the page on screen stands in for one the SDK has not resolved yet. Static because it
 * is set once per page and read by every leaf under it.
 */
internal val LocalWorkflowSkeleton = staticCompositionLocalOf { false }

private object WorkflowSkeletonDefaults {
    private const val PLACEHOLDER_ALPHA = 0.5f

    val color: Color = Color.Gray.copy(alpha = PLACEHOLDER_ALPHA)
    val cornerRadius = 8.dp
}

/**
 * Greys a component out while the page is a skeleton. Applied at the leaves rather than over the
 * whole page, so the real layout still measures and only the content it would show is covered.
 */
@Suppress("ModifierComposable")
@Composable
internal fun Modifier.workflowSkeleton(): Modifier =
    if (LocalWorkflowSkeleton.current) {
        this.placeholder(
            visible = true,
            color = WorkflowSkeletonDefaults.color,
            shape = RoundedCornerShape(WorkflowSkeletonDefaults.cornerRadius),
            highlight = Fade(
                highlightColor = WorkflowSkeletonDefaults.color,
                animationSpec = PlaceholderDefaults.fadeAnimationSpec,
            ),
        )
    } else {
        this
    }
