package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
@ReadOnlyComposable
internal fun isInPreviewMode() = LocalInspectionMode.current
