@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed

@Preview(name = "Size · RTL leading alignment", locale = "ar")
@Composable
private fun SizeRtlLeadingAlignmentPreview() {
    SizeRtlAlignmentPreview(Alignment.Start)
}

@Preview(name = "Size · RTL trailing alignment", locale = "ar")
@Composable
private fun SizeRtlTrailingAlignmentPreview() {
    SizeRtlAlignmentPreview(Alignment.End)
}

@Composable
private fun SizeRtlAlignmentPreview(alignment: Alignment.Horizontal) {
    Box(
        modifier = Modifier
            .requiredSize(width = 200.dp, height = 80.dp)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredWidth(160.dp)
                .requiredHeight(40.dp)
                .size(
                    size = Size(width = Fit(), height = Fixed(40u)),
                    horizontalAlignment = alignment,
                )
                .background(Color.Red)
                .requiredWidth(40.dp),
        )
    }
}
