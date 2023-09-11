package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.revenuecat.purchases.paywalls.PaywallData

@Composable
internal fun IconImage(paywallData: PaywallData) {
    Column(modifier = Modifier.widthIn(max = UIConstant.maxIconWidth)) {
        AsyncImage(
            model = paywallData.iconUrlString,
            contentDescription = null,
            imageLoader = LocalContext.current.getRevenueCatUIImageLoader(),
            modifier = Modifier
                .aspectRatio(ratio = 1f)
                .widthIn(max = UIConstant.maxIconWidth)
                .clip(RoundedCornerShape(UIConstant.iconCornerRadius)),
            contentScale = ContentScale.Crop,
        )
    }
}
