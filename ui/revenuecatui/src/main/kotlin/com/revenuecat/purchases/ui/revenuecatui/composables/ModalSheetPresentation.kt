package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

private const val SCRIM_ALPHA = 0.32f
private const val SHEET_ANIMATION_MILLIS = 300
private const val DISMISS_FRACTION = 0.5f
private val SheetTopMargin = 16.dp
private val SheetCornerRadius = 28.dp
private val DismissVelocityThreshold = 125.dp

internal const val MODAL_SHEET_SCRIM_TEST_TAG = "rc_modal_sheet_scrim"

/**
 * The position of a [ModalSheetPresentation]'s sheet, and the only way to take it down programmatically. Owned
 * by whoever presents the sheet so it outlives any single composition: a sheet re-composed after a configuration
 * change picks up where it was, at rest or mid-hide, without animating in again.
 */
@Stable
internal class ModalSheetState {

    /** How much of the sheet is off screen: 0 at rest, 1 fully hidden. Drives placement and the scrim alone. */
    var hiddenFraction: Float by mutableFloatStateOf(1f)
        private set

    /** Where the sheet is heading: on screen until [hide] is called. */
    var visible: Boolean by mutableStateOf(true)
        private set

    /** The sheet's measured height, read by drags to convert pixels into a fraction. Set at layout time. */
    var heightPx: Int = 0

    private var attached = false
    private var hidden = false
    private var onHidden: (() -> Unit)? = null

    // The enter, exit or settle animation in flight; a drag takes over from a settle or the enter animation.
    private var animation: Job? = null

    /**
     * Slides the sheet off screen and then runs [onHidden], immediately when the sheet is already gone or not
     * composed at all, since nothing could animate it then.
     */
    fun hide(onHidden: () -> Unit) {
        if (hidden || !attached) {
            hidden = true
            onHidden()
            return
        }
        this.onHidden = onHidden
        visible = false
    }

    internal fun attach() {
        attached = true
    }

    internal fun detach() {
        attached = false
    }

    internal fun notifyHidden() {
        hidden = true
        onHidden?.also { onHidden = null }?.invoke()
    }

    /** Moves the sheet by [deltaPx] and returns how much of it was used. Ignored once the sheet is leaving. */
    internal fun dragBy(deltaPx: Float): Float {
        if (!visible || heightPx == 0) return 0f
        animation?.cancel()
        val current = hiddenFraction * heightPx
        val next = (current + deltaPx).coerceIn(0f, heightPx.toFloat())
        hiddenFraction = next / heightPx
        return next - current
    }

    internal suspend fun animateTo(target: Float, initialVelocityPxPerSecond: Float = 0f) {
        if (hiddenFraction == target) return
        animation = coroutineContext.job
        animate(
            initialValue = hiddenFraction,
            targetValue = target,
            initialVelocity = if (heightPx == 0) 0f else initialVelocityPxPerSecond / heightPx,
            animationSpec = tween(SHEET_ANIMATION_MILLIS),
        ) { value, _ -> hiddenFraction = value }
    }

    /** Whether releasing the sheet at its current position with [velocityPxPerSecond] dismisses it. */
    internal fun shouldDismiss(velocityPxPerSecond: Float, velocityThresholdPxPerSecond: Float): Boolean = when {
        velocityPxPerSecond > velocityThresholdPxPerSecond -> true
        velocityPxPerSecond < -velocityThresholdPxPerSecond -> false
        else -> hiddenFraction > DISMISS_FRACTION
    }
}

/**
 * A modal sheet in the style of a system page sheet: [content] sits in a full-height card below the status bar,
 * over a scrim, and slides in from the bottom on first composition. Tapping the scrim or swiping the sheet down past
 * half its height (or flinging it) calls [onDismissRequest] once the sheet is off screen; what happens then is the
 * caller's call. Programmatic dismissal goes through [ModalSheetState.hide].
 *
 * The content gets bounded constraints (the sheet's size) and no top window inset, since the sheet already sits
 * below the status bar. Bottom and horizontal insets pass through. A vertically scrollable content scrolls first;
 * the sheet only follows a downward drag once the content is at its top.
 */
@Composable
internal fun ModalSheetPresentation(
    state: ModalSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val velocityThresholdPx = with(LocalDensity.current) { DismissVelocityThreshold.toPx() }

    DisposableEffect(state) {
        state.attach()
        onDispose { state.detach() }
    }

    LaunchedEffect(state, state.visible) {
        state.animateTo(if (state.visible) 0f else 1f)
        if (!state.visible) state.notifyHidden()
    }

    val settle: suspend (velocityPxPerSecond: Float) -> Unit = { velocity ->
        if (state.shouldDismiss(velocity, velocityThresholdPx)) {
            state.animateTo(1f, velocity)
            onDismissRequest()
        } else {
            state.animateTo(0f, velocity)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black.copy(alpha = SCRIM_ALPHA * (1f - state.hiddenFraction))) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = state.visible,
                    onClick = onDismissRequest,
                )
                .testTag(MODAL_SHEET_SCRIM_TEST_TAG),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = SheetTopMargin)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    state.heightPx = placeable.height
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, (state.hiddenFraction * placeable.height).roundToInt())
                    }
                }
                .nestedScroll(remember(state) { SheetNestedScrollConnection(state, settle) })
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> state.dragBy(delta) },
                    onDragStopped = { velocity -> settle(velocity) },
                )
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)),
        ) {
            content()
        }
    }
}

/**
 * Lets a vertically scrollable content and the sheet share drags the way a bottom sheet does: while the sheet is
 * pulled down, dragging up brings it back before the content scrolls; a downward drag the content could not
 * consume (it is at its top) pulls the sheet instead; a release while the sheet is displaced settles it.
 */
private class SheetNestedScrollConnection(
    private val state: ModalSheetState,
    private val settle: suspend (velocityPxPerSecond: Float) -> Unit,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput && available.y < 0f && state.hiddenFraction > 0f) {
            Offset(0f, state.dragBy(available.y))
        } else {
            Offset.Zero
        }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput && available.y > 0f) {
            Offset(0f, state.dragBy(available.y))
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity =
        if (state.hiddenFraction > 0f) {
            settle(available.y)
            available
        } else {
            Velocity.Zero
        }
}
