@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import java.util.Date

@Composable
internal fun CountdownComponentView(
    style: CountdownComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdownState = rememberCountdownState(style.date)

    val stackStyle = if (countdownState.hasEnded && style.endStackComponentStyle != null) {
        style.endStackComponentStyle
    } else {
        style.countdownStackComponentStyle
    }
    StackComponentView(
        style = stackStyle,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
}

@Suppress("MagicNumber")
@Preview(name = "CountdownRunning")
@Composable
private fun CountdownComponentView_Preview_Running() {
    val countdownDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)
    val countdownTextStyle = previewTextComponentStyle(
        text = "{{ count_days_without_zero }}d {{ count_hours_without_zero }}h " +
            "{{ count_minutes_without_zero }}m {{ count_seconds_without_zero }}s",
        color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
        fontSize = 24,
        countdownDate = countdownDate,
    )
    CountdownComponentView(
        style = CountdownComponentStyle(
            date = countdownDate,
            countFrom = CountdownComponent.CountFrom.DAYS,
            countdownStackComponentStyle = previewStackComponentStyle(
                children = listOf(countdownTextStyle),
                countdownDate = countdownDate,
            ),
            endStackComponentStyle = null,
            fallbackStackComponentStyle = null,
        ),
        state = previewEmptyState(),
        onClick = {},
    )
}

@Suppress("MagicNumber")
@Preview(name = "CountdownTimesUp")
@Composable
private fun CountdownComponentView_Preview_TimesUp() {
    val countdownDate = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000)
    val countdownTextStyle = previewTextComponentStyle(
        text = "{{ count_days_without_zero }}d {{ count_hours_without_zero }}h " +
            "{{ count_minutes_without_zero }}m {{ count_seconds_without_zero }}s",
        color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
        fontSize = 24,
        countdownDate = countdownDate,
    )
    val endTextStyle = previewTextComponentStyle(
        text = "Offer expired!",
        color = ColorStyles(light = ColorStyle.Solid(Color.Black)),
        fontSize = 24,
    )
    CountdownComponentView(
        style = CountdownComponentStyle(
            date = countdownDate,
            countFrom = CountdownComponent.CountFrom.DAYS,
            countdownStackComponentStyle = previewStackComponentStyle(
                children = listOf(countdownTextStyle),
                countdownDate = countdownDate,
            ),
            endStackComponentStyle = previewStackComponentStyle(
                children = listOf(endTextStyle),
            ),
            fallbackStackComponentStyle = null,
        ),
        state = previewEmptyState(),
        onClick = {},
    )
}
