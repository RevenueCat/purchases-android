package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

@Composable
internal fun TierSwitcher(
    tiers: List<TemplateConfiguration.TierInfo>,
    selectedTier: TemplateConfiguration.TierInfo,
    onTierSelected: (TemplateConfiguration.TierInfo) -> Unit,
    backgroundColor: Color,
) {
    val selectedIndex = tiers.indexOf(selectedTier)
    var totalWidthPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current

    Box(
        Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF0F0F0))
            .height(40.dp)
            .fillMaxWidth()
            .onSizeChanged { size ->
                totalWidthPx = size.width
            },
    ) {
        val optionWidth = with(density) { (totalWidthPx / tiers.size).toDp() }
        val indicatorOffset by animateDpAsState(targetValue = optionWidth * selectedIndex)

        Box(
            Modifier
                .offset(x = indicatorOffset)
                .fillMaxHeight()
                .width(optionWidth)
                .clip(RoundedCornerShape(50))
                .background(backgroundColor),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize(),
        ) {
            tiers.forEach { tier ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onTierSelected(tier)
                        },
                ) {
                    Text(
                        text = tier.name,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = if (selectedTier == tier) Color.White else Color.Black,
                    )
                }
            }
        }
    }
}
