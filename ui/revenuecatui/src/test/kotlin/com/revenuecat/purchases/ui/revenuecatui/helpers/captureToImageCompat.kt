package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.DoNotInline
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.graphics.HardwareRendererCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Capture this [SemanticsNodeInteraction] to an [ImageBitmap], regardless of whether this test runs on Android or the
 * JVM.
 *
 * When running on the JVM, make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 */
internal fun SemanticsNodeInteraction.captureToImageCompat(): ImageBitmap =
    if (System.getProperty("java.runtime.name").orEmpty().lowercase().contains("android")) captureToImage()
    else captureToImageJvm()

/**
 * Robolectric-compatible way of capturing a [SemanticsNodeInteraction] to an [ImageBitmap]. It's a copy from
 * Google's version, minus a forced redraw which doesn't work on Robolectric.
 *
 * Make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 *
 * See [this issue](https://github.com/robolectric/robolectric/issues/8071) for more info.
 *
 * [Source](https://github.com/robolectric/robolectric/issues/8071#issuecomment-1774365222)
 */
@OptIn(ExperimentalTestApi::class)
@SdkSuppress(minSdkVersion = 26)
private fun SemanticsNodeInteraction.captureToImageJvm(): ImageBitmap {
    val node = fetchSemanticsNode("Failed to capture a node to bitmap.")
    // Validate we are in popup
    val popupParentMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsPopup)
    }
    if (popupParentMaybe != null) {
        return processMultiWindowScreenshot(node)
    }

    val view = (node.root as ViewRootForTest).view

    // If we are in dialog use its window to capture the bitmap
    val dialogParentNodeMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsDialog)
    }
    var dialogWindow: Window? = null
    if (dialogParentNodeMaybe != null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // (b/163023027)
            error("Cannot currently capture dialogs on API lower than 28!")
        }

        dialogWindow = findDialogWindowProviderInParent(view)?.window
            ?: error(
                "Could not find a dialog window provider to capture its bitmap"
            )
    }

    val windowToUse = dialogWindow ?: view.context.getActivityWindow()

    val nodeBounds = node.boundsInRoot
    val nodeBoundsRect = Rect(
        nodeBounds.left.roundToInt(),
        nodeBounds.top.roundToInt(),
        nodeBounds.right.roundToInt(),
        nodeBounds.bottom.roundToInt()
    )

    val locationInWindow = intArrayOf(0, 0)
    view.getLocationInWindow(locationInWindow)
    val x = locationInWindow[0]
    val y = locationInWindow[1]

    // Now these are bounds in window
    nodeBoundsRect.offset(x, y)

    return windowToUse.captureRegionToImage(nodeBoundsRect)
}

@SdkSuppress(minSdkVersion = 26)
private fun SemanticsNode.findClosestParentNode(
    includeSelf: Boolean = false,
    selector: (SemanticsNode) -> Boolean,
): SemanticsNode? {
    var currentParent = if (includeSelf) this else parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

@ExperimentalTestApi
@SdkSuppress(minSdkVersion = 26)
private fun processMultiWindowScreenshot(
    node: SemanticsNode,
): ImageBitmap {
    val nodePositionInScreen = findNodePosition(node)
    val nodeBoundsInRoot = node.boundsInRoot

    val combinedBitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()

    val finalBitmap = Bitmap.createBitmap(
        combinedBitmap,
        (nodePositionInScreen.x + nodeBoundsInRoot.left).roundToInt(),
        (nodePositionInScreen.y + nodeBoundsInRoot.top).roundToInt(),
        nodeBoundsInRoot.width.roundToInt(),
        nodeBoundsInRoot.height.roundToInt()
    )
    return finalBitmap.asImageBitmap()
}

private fun findNodePosition(
    node: SemanticsNode,
): Offset {
    val view = (node.root as ViewRootForTest).view
    val locationOnScreen = intArrayOf(0, 0)
    view.getLocationOnScreen(locationOnScreen)
    val x = locationOnScreen[0]
    val y = locationOnScreen[1]

    return Offset(x.toFloat(), y.toFloat())
}

@Suppress("ReturnCount")
private fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
    if (view is DialogWindowProvider) {
        return view
    }
    val parent = view.parent ?: return null
    if (parent is View) {
        return findDialogWindowProviderInParent(parent)
    }
    return null
}

private fun Context.getActivityWindow(): Window {
    fun Context.getActivity(): Activity {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> throw IllegalStateException(
                "Context is not an Activity context, but a ${javaClass.simpleName} context. " +
                    "An Activity context is required to get a Window instance"
            )
        }
    }
    return getActivity().window
}

@SdkSuppress(minSdkVersion = 26)
private fun Window.captureRegionToImage(
    boundsInWindow: Rect,
): ImageBitmap {
    // Turn on hardware rendering, if necessary
    return withDrawingEnabled {
        // Then we generate the bitmap
        generateBitmap(boundsInWindow).asImageBitmap()
    }
}

private fun <R> withDrawingEnabled(block: () -> R): R {
    val wasDrawingEnabled = HardwareRendererCompat.isDrawingEnabled()
    try {
        if (!wasDrawingEnabled) {
            HardwareRendererCompat.setDrawingEnabled(true)
        }
        return block.invoke()
    } finally {
        if (!wasDrawingEnabled) {
            HardwareRendererCompat.setDrawingEnabled(false)
        }
    }
}

@SdkSuppress(minSdkVersion = 26)
private fun Window.generateBitmap(boundsInWindow: Rect): Bitmap {
    val destBitmap =
        Bitmap.createBitmap(
            boundsInWindow.width(),
            boundsInWindow.height(),
            Bitmap.Config.ARGB_8888
        )
    generateBitmapFromPixelCopy(boundsInWindow, destBitmap)
    return destBitmap
}

@SdkSuppress(minSdkVersion = 26)
private object PixelCopyHelper {
    @DoNotInline
    fun request(
        source: Window,
        srcRect: Rect?,
        dest: Bitmap,
        listener: PixelCopy.OnPixelCopyFinishedListener,
        listenerThread: Handler,
    ) {
        PixelCopy.request(source, srcRect, dest, listener, listenerThread)
    }
}

@SdkSuppress(minSdkVersion = 26)
private fun Window.generateBitmapFromPixelCopy(boundsInWindow: Rect, destBitmap: Bitmap) {
    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = PixelCopy.OnPixelCopyFinishedListener { result ->
        copyResult = result
        latch.countDown()
    }
    PixelCopyHelper.request(
        this,
        boundsInWindow,
        destBitmap,
        onCopyFinished,
        Handler(Looper.getMainLooper())
    )

    if (!latch.await(1, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for PixelCopy!")
    }
    if (copyResult != PixelCopy.SUCCESS) {
        throw AssertionError("PixelCopy failed!")
    }
}
