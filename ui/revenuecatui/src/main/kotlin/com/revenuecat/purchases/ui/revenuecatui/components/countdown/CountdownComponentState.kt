@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.window.core.layout.WindowWidthSizeClass
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ConditionContext
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageAwareDelegate
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore
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

@Stable
@JvmSynthetic
@Composable
internal fun rememberUpdatedCountdownComponentState(
    style: CountdownComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): CountdownComponentState = rememberUpdatedCountdownComponentState(
    style = style,
    windowDpSize = paywallState.paywallBoundsDp,
    selectedPackageInfoProvider = { paywallState.selectedPackageInfo },
    selectedTabIndexProvider = { paywallState.selectedTabIndex },
    selectedOfferEligibilityProvider = { paywallState.selectedOfferEligibility },
    customVariablesProvider = { paywallState.mergedCustomVariables },
    stateStoreProvider = { paywallState.stateStore },
)

@Suppress("LongParameterList")
@Stable
@JvmSynthetic
@Composable
private fun rememberUpdatedCountdownComponentState(
    style: CountdownComponentStyle,
    windowDpSize: DpSize?,
    selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    selectedTabIndexProvider: () -> Int,
    selectedOfferEligibilityProvider: () -> OfferEligibility,
    customVariablesProvider: () -> Map<String, CustomVariableValue>,
    stateStoreProvider: () -> PaywallStateStore,
): CountdownComponentState {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    return remember(style) {
        CountdownComponentState(
            initialWindowSize = windowSize,
            initialWindowDpSize = windowDpSize,
            style = style,
            selectedPackageInfoProvider = selectedPackageInfoProvider,
            selectedTabIndexProvider = selectedTabIndexProvider,
            selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
            customVariablesProvider = customVariablesProvider,
            stateStoreProvider = stateStoreProvider,
        )
    }.apply {
        update(windowSize = windowSize, windowDpSize = windowDpSize)
    }
}

@Suppress("LongParameterList")
@Stable
internal class CountdownComponentState(
    initialWindowSize: WindowWidthSizeClass,
    initialWindowDpSize: DpSize?,
    private val style: CountdownComponentStyle,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
    private val customVariablesProvider: () -> Map<String, CustomVariableValue> = { emptyMap() },
    private val stateStoreProvider: () -> PaywallStateStore = { PaywallStateStore(emptyMap()) },
) {
    private var windowSize by mutableStateOf(initialWindowSize)
    private var windowDpSize by mutableStateOf(initialWindowDpSize)

    private val packageAwareDelegate = PackageAwareDelegate(
        style = style,
        selectedPackageInfoProvider = selectedPackageInfoProvider,
        selectedTabIndexProvider = selectedTabIndexProvider,
        selectedOfferEligibilityProvider = selectedOfferEligibilityProvider,
    )

    private val presentedPartial by derivedStateOf {
        val windowCondition = ScreenCondition.from(windowSize)
        val componentState =
            if (packageAwareDelegate.isSelected) ComponentViewState.SELECTED else ComponentViewState.DEFAULT

        style.overrides.buildPresentedPartial(
            windowCondition,
            packageAwareDelegate.offerEligibility,
            componentState,
            conditionContext = ConditionContext(
                selectedPackageId = selectedPackageInfoProvider()?.rcPackage?.identifier,
                customVariables = customVariablesProvider(),
                stateReader = stateStoreProvider()::currentValueOrDefault,
                windowDpSize = windowDpSize,
            ),
        )
    }

    @get:JvmSynthetic
    val visible by derivedStateOf { presentedPartial?.partial?.visible ?: style.visible }

    @JvmSynthetic
    fun update(windowSize: WindowWidthSizeClass? = null, windowDpSize: DpSize? = null) {
        if (windowSize != null) this.windowSize = windowSize
        if (windowDpSize != null) this.windowDpSize = windowDpSize
    }
}
