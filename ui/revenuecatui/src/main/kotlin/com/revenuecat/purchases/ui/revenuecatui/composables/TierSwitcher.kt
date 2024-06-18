package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.animateColorAsState
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
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

private object TierSwitcherUIConstants {
    const val roundedCorner = 50
    val height = 40.dp
    val selectedTierPadding = 2.dp
    val tierHorizontalPadding = 16.dp
    val tierVerticalPadding = 8.dp
}

@Composable
@Suppress("LongParameterList")
internal fun SelectedTierView(
    selectedTier: TemplateConfiguration.TierInfo,
    backgroundSelectedColor: Color,
    foregroundSelectedColor: Color,
) {
    Box(
        modifier = Modifier
            .background(
                color = backgroundSelectedColor,
                shape = RoundedCornerShape(TierSwitcherUIConstants.roundedCorner),
            )
            .padding(
                horizontal = TierSwitcherUIConstants.tierHorizontalPadding,
                vertical = TierSwitcherUIConstants.tierVerticalPadding,
            ),
    ) {
        Text(
            text = selectedTier.name,
            color = foregroundSelectedColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun TierSwitcher(
    tiers: List<TemplateConfiguration.TierInfo>,
    selectedTier: TemplateConfiguration.TierInfo,
    onTierSelected: (TemplateConfiguration.TierInfo) -> Unit,
    backgroundColor: Color,
    backgroundSelectedColor: Color,
    foregroundColor: Color,
    foregroundSelectedColor: Color,
) {
    val selectedIndex = tiers.indexOf(selectedTier)
    var totalWidthPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current

    val backgroundColorState by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = UIConstant.defaultColorAnimation,
        label = "backgroundColor",
    )
    val backgroundSelectedColorState by animateColorAsState(
        targetValue = backgroundSelectedColor,
        animationSpec = UIConstant.defaultColorAnimation,
        label = "backgroundSelectedColor",
    )
    val foregroundColorState by animateColorAsState(
        targetValue = foregroundColor,
        animationSpec = UIConstant.defaultColorAnimation,
        label = "foregroundColor",
    )
    val foregroundSelectedColorState by animateColorAsState(
        targetValue = foregroundSelectedColor,
        animationSpec = UIConstant.defaultColorAnimation,
        label = "foregroundSelectedColor",
    )

    Box(
        Modifier
            .padding(TierSwitcherUIConstants.selectedTierPadding)
            .clip(RoundedCornerShape(TierSwitcherUIConstants.roundedCorner))
            .background(backgroundColorState)
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
                .fillMaxWidth(1f / tiers.size)
                .padding(TierSwitcherUIConstants.selectedTierPadding)
                .clip(RoundedCornerShape(TierSwitcherUIConstants.roundedCorner))
                .background(backgroundSelectedColorState),
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
                        color = if (selectedTier == tier) foregroundSelectedColorState else foregroundColorState,
                    )
                }
            }
        }
    }
}
