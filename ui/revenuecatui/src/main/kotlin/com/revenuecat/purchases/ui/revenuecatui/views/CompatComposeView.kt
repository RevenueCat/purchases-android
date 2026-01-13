package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * A ComposeView expects a few things to be set up in the view tree it is added to. This gets handled automatically by
 * modern parents such as ComponentActivity and androidx Fragment. But this is not always the case, such as when the
 * ComposeView is added to a plain Window, or in certain hybrid frameworks. A [CompatComposeView] can handle this
 * scenario by acting as its own LifecycleOwner, SavedStateRegistryOwner and ViewModelStoreOwner if required.
 *
 * Note that, in this scenario, this does imply that the ComposeView's lifecycle is tied to its visibility. It ends
 * when it is removed from the view tree.
 */
@Suppress("TooManyFunctions")
@InternalRevenueCatAPI
abstract class CompatComposeView @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private companion object {
        private const val KEY_SAVED_INSTANCE_STATE = "com.revenuecat.CompatComposeView.saved_instance_state"
    }

    /**
     * A LifecycleOwner that derives its lifecycle state from View attachment and visibility callbacks.
     * Used internally by [CompatComposeView] when no external LifecycleOwner is provided.
     *
     * **Note:** there is no destroy signal for Views.
     */
    private class ViewLifecycleOwner : LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun onAttachedToWindow() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onWindowVisibilityChanged(visibility: Int) {
            if (visibility == VISIBLE) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            } else {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }

        fun onWindowFocusChanged(hasWindowFocus: Boolean) {
            if (hasWindowFocus) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            } else {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
        }

        fun onDetachedFromWindow() {
            // Only goes to ON_STOP, not ON_DESTROY as the view might be reattached later.
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    private var isManagingLifecycle = false
    private var isManagingSavedState = false
    private var isManagingViewModelStore = false
    private val isManagingViewTree: Boolean
        get() = isManagingLifecycle || isManagingSavedState || isManagingViewModelStore

    private val lifecycleOwner: LifecycleOwner = ViewLifecycleOwner()
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleOwner.lifecycle
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    open fun onBackPressed() {
        (parent as? ViewGroup)?.removeView(this)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val state = super.onSaveInstanceState()
        if (isManagingSavedState) performSave(state)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        if (isManagingSavedState) performRestore(state)
    }

    override fun onAttachedToWindow() {
        initViewTreeOwners()
        if (isManagingSavedState) {
            savedStateRegistryController.performAttach()
            performRestore(null)
        }
        (lifecycleOwner as? ViewLifecycleOwner)?.onAttachedToWindow()
        super.onAttachedToWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        (lifecycleOwner as? ViewLifecycleOwner)?.onWindowVisibilityChanged(visibility)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isManagingViewTree) {
            // Make focusable and request focus, to be able to intercept back button presses.
            isFocusableInTouchMode = true
            isFocusable = true
            requestFocus()
        }
        (lifecycleOwner as? ViewLifecycleOwner)?.onWindowFocusChanged(hasWindowFocus)
    }

    override fun onDetachedFromWindow() {
        (lifecycleOwner as? ViewLifecycleOwner)?.onDetachedFromWindow()
        if (isManagingViewModelStore) viewModelStore.clear()
        deinitViewTreeOwners()
        super.onDetachedFromWindow()
    }

    @Suppress("ReturnCount")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isManagingViewTree) return super.dispatchKeyEvent(event)
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun performSave(state: Parcelable?): Bundle {
        val bundle = Bundle().apply { putParcelable(KEY_SAVED_INSTANCE_STATE, state) }
        savedStateRegistryController.performSave(bundle)
        return bundle
    }

    private fun performRestore(state: Parcelable?) {
        val bundle = Bundle().apply { putParcelable(KEY_SAVED_INSTANCE_STATE, state) }
        savedStateRegistryController.performRestore(bundle)
    }

    private fun initViewTreeOwners() {
        val windowRoot = findWindowRoot() ?: return

        // Depending on the host environment, some of these may already be set up. E.g. in Expo < 54 all three are
        // null, so we set up all of them. In Expo 54+, LifecycleOwner is already set up, but SavedStateRegistryOwner
        // and ViewModelStoreOwner are not. We track each one separately to avoid performing operations on owners we
        // didn't set up.
        if (windowRoot.findViewTreeLifecycleOwner() == null) {
            windowRoot.setViewTreeLifecycleOwner(this)
            isManagingLifecycle = true
        }
        if (windowRoot.findViewTreeSavedStateRegistryOwner() == null) {
            windowRoot.setViewTreeSavedStateRegistryOwner(this)
            isManagingSavedState = true
        }
        if (windowRoot.findViewTreeViewModelStoreOwner() == null) {
            windowRoot.setViewTreeViewModelStoreOwner(this)
            isManagingViewModelStore = true
        }
    }

    private fun deinitViewTreeOwners() {
        if (!isManagingViewTree) return
        val windowRoot = findWindowRoot() ?: return

        if (windowRoot.findViewTreeLifecycleOwner() === this) {
            windowRoot.setViewTreeLifecycleOwner(null)
        }
        if (windowRoot.findViewTreeSavedStateRegistryOwner() === this) {
            windowRoot.setViewTreeSavedStateRegistryOwner(null)
        }
        if (windowRoot.findViewTreeViewModelStoreOwner() === this) {
            windowRoot.setViewTreeViewModelStoreOwner(null)
        }
    }

    private fun View.findWindowRoot(): View? {
        // The ultimate root of a window is android.view.ViewRootImpl, but that's a private type. This ViewRootImpl
        // has 1 child, which is a regular ViewGroup we can work with.
        var lastViewGroup: ViewGroup? = null
        var currentParent = parent

        while (currentParent != null) {
            if (currentParent !is ViewGroup) break

            lastViewGroup = currentParent
            currentParent = currentParent.parent
        }

        return lastViewGroup
    }
}
