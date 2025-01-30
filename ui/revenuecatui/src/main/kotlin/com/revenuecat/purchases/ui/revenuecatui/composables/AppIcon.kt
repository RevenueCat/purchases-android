package com.revenuecat.purchases.ui.revenuecatui.composables

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
internal fun AppIcon(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val appIconResId = remember {
        val packageManager = context.packageManager
        context.applicationInfo.loadIcon(packageManager)
    }
    androidx.compose.foundation.Image(
        bitmap = appIconResId.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}
