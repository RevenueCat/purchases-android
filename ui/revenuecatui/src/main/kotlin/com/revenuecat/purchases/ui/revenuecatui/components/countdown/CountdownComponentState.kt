@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

private const val SECONDS_IN_DAY = 86_400
private const val SECONDS_IN_HOUR = 3_600
private const val SECONDS_IN_MINUTE = 60
private const val MILLIS_IN_SECOND = 1000

internal data class CountdownTime(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    @Suppress("MagicNumber")
    val totalHours: Int
        get() = days * 24 + hours

    @Suppress("MagicNumber")
    val totalMinutes: Int
        get() = days * 24 * 60 + hours * 60 + minutes

    companion object {
        val ZERO = CountdownTime(0, 0, 0, 0)

        fun fromInterval(interval: Long): CountdownTime {
            val totalSeconds = maxOf(0, interval / MILLIS_IN_SECOND)

            val days = (totalSeconds / SECONDS_IN_DAY).toInt()
            val hours = ((totalSeconds % SECONDS_IN_DAY) / SECONDS_IN_HOUR).toInt()
            val minutes = ((totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE).toInt()
            val seconds = (totalSeconds % SECONDS_IN_MINUTE).toInt()

            return CountdownTime(days, hours, minutes, seconds)
        }
    }
}

@Composable
internal fun rememberCountdownState(targetDate: Date): CountdownState {
    val initialDelta = remember(targetDate) { targetDate.time - Date().time }
    var countdownTime by remember(targetDate) {
        mutableStateOf(
            if (initialDelta <= 0) CountdownTime.ZERO else CountdownTime.fromInterval(initialDelta),
        )
    }
    var isCountingEnabled by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(targetDate) {
        launch {
            lifecycleOwner.lifecycle.currentStateFlow
                .map { it.isAtLeast(Lifecycle.State.STARTED) }
                .distinctUntilChanged()
                .collect { isStarted ->
                    isCountingEnabled = isStarted
                }
        }

        while (coroutineContext.isActive) {
            if (isCountingEnabled) {
                val now = Date().time
                val delta = targetDate.time - now

                if (delta <= 0) {
                    countdownTime = CountdownTime.ZERO
                    break
                }

                countdownTime = CountdownTime.fromInterval(delta)
            }

            delay(1.seconds)
        }
    }

    return CountdownState(
        countdownTime = countdownTime,
        hasEnded = countdownTime == CountdownTime.ZERO,
    )
}

internal data class CountdownState(
    val countdownTime: CountdownTime,
    val hasEnded: Boolean,
)
