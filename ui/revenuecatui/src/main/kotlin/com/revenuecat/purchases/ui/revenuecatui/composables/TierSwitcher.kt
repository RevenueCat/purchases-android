package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

private object TierSwitcherUIConstants {
    const val roundedCorner = 50
    val height = 40.dp
}

@Composable
internal fun TierSwitcher(
    tiers: List<TemplateConfiguration.TierInfo>,
    selectedTier: TemplateConfiguration.TierInfo,
    onTierSelected: (TemplateConfiguration.TierInfo) -> Unit,
    backgroundColor: Color,
    accentColor: Color,
    textColor: Color,
    textSelectedColor: Color,
) {
    val selectedIndex = tiers.indexOf(selectedTier)
    var totalWidthPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current

    Box(
        Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(TierSwitcherUIConstants.roundedCorner))
            .background(backgroundColor)
            .height(TierSwitcherUIConstants.height)
            .fillMaxWidth()
            .onSizeChanged { size ->
                totalWidthPx = size.width
            },
    ) {
        val optionWidth = with(density) { (totalWidthPx / tiers.size).toDp() }
        val indicatorOffset by animateDpAsState(targetValue = optionWidth * selectedIndex, label = "tier_switcher")

        Box(
            Modifier
                .offset(x = indicatorOffset)
                .fillMaxHeight()
                .width(optionWidth)
                .clip(RoundedCornerShape(50))
                .background(accentColor),
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
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (selectedTier == tier) textSelectedColor else textColor,
                    )
                }
            }
        }
    }
}
