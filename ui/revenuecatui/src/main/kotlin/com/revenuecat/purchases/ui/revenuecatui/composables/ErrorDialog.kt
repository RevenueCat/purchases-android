package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.ui.revenuecatui.R

@Composable
internal fun ErrorDialog(
    dismissRequest: () -> Unit,
    error: String,
) {
    AlertDialog(
        onDismissRequest = dismissRequest,
        confirmButton = {
            TextButton(
                onClick = dismissRequest,
            ) {
                Text(
                    text = stringResource(id = R.string.OK),
                    textAlign = TextAlign.Center,
                )
            }
        },
        icon = { Icon(painter = painterResource(id = R.drawable.error), contentDescription = null) },
        text = {
            Text(text = error)
        },
    )
}
