package com.revenuecat.purchases.ui.revenuecatui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.revenuecat.purchases.paywalls.PaywallData
import java.net.URL

@Composable
internal fun IconImage(paywallData: PaywallData) {
    Column(modifier = Modifier.widthIn(max = UIConstant.maxIconWidth)) {
        AsyncImage(
            // TODO-PAYWALLS: Extract to paywall data building code
            url = URL(
                Uri.parse(paywallData.assetBaseURL.toString()).buildUpon().path(
                    paywallData.config.images.icon ?: "",
                ).build().toString(),
            ),
            modifier = Modifier
                .aspectRatio(ratio = 1f)
                .widthIn(max = UIConstant.maxIconWidth)
                .clip(RoundedCornerShape(UIConstant.iconCornerRadius)),
            contentScale = ContentScale.Crop,
        )
    }
}
