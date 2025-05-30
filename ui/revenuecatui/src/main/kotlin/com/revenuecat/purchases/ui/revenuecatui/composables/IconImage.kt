package com.revenuecat.purchases.ui.revenuecatui.composables

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultAppIconPlaceholder
import com.revenuecat.purchases.ui.revenuecatui.helpers.isInPreviewMode

@Composable
internal fun IconImage(
    uri: Uri?,
    maxWidth: Dp,
    iconCornerRadius: Dp,
    childModifier: Modifier = Modifier,
) {
    uri?.let {
        Column(modifier = Modifier.widthIn(max = maxWidth)) {
            val modifier = childModifier
                .aspectRatio(ratio = 1f)
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(iconCornerRadius))
            if (isInPreviewMode()) {
                Box(
                    modifier = modifier
                        .background(color = MaterialTheme.colorScheme.primary)
                        .size(maxWidth),
                )
            } else if (uri.toString().contains(PaywallData.defaultAppIconPlaceholder)) {
                AppIcon(modifier = modifier)
            } else {
                RemoteImage(
                    urlString = uri.toString(),
                    modifier = modifier,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Preview
@Composable
private fun IconImagePreview() {
    IconImage(
        uri = Uri.parse("https://assets.pawwalls.com/icon.jpg"),
        maxWidth = 140.dp,
        iconCornerRadius = 16.dp,
    )
}
