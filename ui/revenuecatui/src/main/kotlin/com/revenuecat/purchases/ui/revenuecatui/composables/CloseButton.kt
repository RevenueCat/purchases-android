package com.revenuecat.purchases.ui.revenuecatui.composables

import android.util.Log
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.revenuecat.purchases.ui.revenuecatui.R

@Composable
fun BoxScope.CloseButton(
    shouldDisplayDismissButton: Boolean,
    actionInProgress: Boolean,
    onClick: () -> Unit,
) {
    if (shouldDisplayDismissButton) {
        // Use Accompanist Insets (if available) or a similar method to obtain the top padding equivalent to the status bar height
        val view = LocalView.current
        val topPadding = with(LocalDensity.current) {
            ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top?.toDp() ?: 0.dp
        }
        IconButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.TopStart),
            enabled = actionInProgress.not(),
        ) {
            Icon(painter = painterResource(id = R.drawable.close), contentDescription = null)
        }
    }
}
