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
import com.revenuecat.purchases.paywalls.PaywallData

@Composable
internal fun PillSwitcher(
    options: List<Pair<PaywallData.Configuration.Tier, String>>,
    selectedOption: PaywallData.Configuration.Tier,
    onOptionSelected: (PaywallData.Configuration.Tier) -> Unit,
    backgroundColor: Color,
) {
    val values = options.map { it.first }
    val selectedIndex = values.indexOf(selectedOption)
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
        val optionWidth = with(density) { (totalWidthPx / options.size).toDp() }
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
            options.forEach { option ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            onOptionSelected(option.first)
                        },
                ) {
                    Text(
                        text = option.second, // TODO: this is also bad
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = if (selectedOption == option.first) Color.White else Color.Black,
                    )
                }
            }
        }
    }
}
