@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import dev.drewhamilton.poko.Poko

@Poko
@Immutable
internal class AspectRatio(
    @get:JvmSynthetic val ratio: Float,
    @get:JvmSynthetic val matchHeightConstraintsFirst: Boolean,
)

@JvmSynthetic
@Stable
internal fun Modifier.aspectRatio(
    aspectRatio: AspectRatio,
): Modifier =
    this then Modifier.aspectRatio(
        ratio = aspectRatio.ratio,
        matchHeightConstraintsFirst = aspectRatio.matchHeightConstraintsFirst,
    )
