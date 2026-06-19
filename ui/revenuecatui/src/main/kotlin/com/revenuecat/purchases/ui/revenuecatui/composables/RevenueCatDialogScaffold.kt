package com.revenuecat.purchases.ui.revenuecatui.composables

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.hasCompactDimension
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowAspectRatio

private object DialogSizingConstants {
    const val MAX_HEIGHT_PERCENTAGE_TABLET = 0.85f
    const val MAX_ASPECT_RATIO_TO_APPLY_MAX_HEIGHT = 1.25f
}

@Composable
internal fun RevenueCatDialogScaffold(
    handleCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dialogBottomPadding = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    BackHandler(onBack = handleCloseRequest)

    Dialog(
        onDismissRequest = handleCloseRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = shouldUsePlatformDefaultWidth(),
            decorFitsSystemWindows = Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        ),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(getDialogMaxHeightPercentage()),
            // This is needed for Android 35+ but using an older version of Compose. In those cases,
            // the dialog doesn't properly extend edge to edge, leaving some spacing at the bottom since we changed
            // the decorFitsSystemWindows setting of the Dialog. This is added to mimic the dim effect that we get
            // at the top of the dialog in this case. This should be removed once we update Compose in the next major.
            containerColor = Color.Black.copy(alpha = 0.4f),
        ) { paddingValues ->
            val shouldApplyDialogBottomPadding = paddingValues.calculateBottomPadding() == 0.dp &&
                paddingValues.calculateTopPadding() == 0.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .conditional(
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                    ) { padding(paddingValues) }
                    .padding(bottom = if (shouldApplyDialogBottomPadding) dialogBottomPadding else 0.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
@ReadOnlyComposable
internal fun getDialogMaxHeightPercentage(): Float {
    if (windowAspectRatio() < DialogSizingConstants.MAX_ASPECT_RATIO_TO_APPLY_MAX_HEIGHT) {
        return 1f
    }
    return if (hasCompactDimension()) 1f else DialogSizingConstants.MAX_HEIGHT_PERCENTAGE_TABLET
}

@Composable
@ReadOnlyComposable
internal fun shouldUsePlatformDefaultWidth(): Boolean {
    return !hasCompactDimension()
}
