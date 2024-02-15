package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.revenuecat.purchases.ui.revenuecatui.R

@Composable
fun BoxScope.CloseButton(
    shouldDisplayDismissButton: Boolean,
    actionInProgress: Boolean,
    onClick: () -> Unit,
) {
    if (shouldDisplayDismissButton) {
        Column(
            Modifier.align(Alignment.TopStart),
        ) {
            StatusBarSpacer()
            IconButton(
                onClick = onClick,
                enabled = actionInProgress.not(),
            ) {
                Icon(painter = painterResource(id = R.drawable.close), contentDescription = null)
            }
        }
    }
}
