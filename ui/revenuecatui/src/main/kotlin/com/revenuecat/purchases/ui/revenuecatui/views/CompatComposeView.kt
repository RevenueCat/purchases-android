package com.revenuecat.purchases.ui.revenuecatui.views

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
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
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity

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
     * Used internally by [CompatComposeView] when no external LifecycleOwner exists in the view tree.
     *
     * @param activity There's no definitive destroy signal for Views, so we'll use the Activity as a last resort.
     */
    private class ViewLifecycleOwner(private val activity: Activity?) : LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

        init {
            activity?.let { act ->
                @Suppress("EmptyFunctionBlock")
                activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityDestroyed(destroyedActivity: Activity) {
                        if (destroyedActivity === act) {
                            destroy()
                        }
                    }
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                    override fun onActivityStarted(activity: Activity) {}
                    override fun onActivityResumed(activity: Activity) {}
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                }.also { callbacks ->
                    act.application?.registerActivityLifecycleCallbacks(callbacks)
                }
            }
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun onAttachedToWindow() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onDetachedFromWindow() {
            destroy()
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

        fun destroy() {
            if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return

            activityLifecycleCallbacks?.let { callbacks ->
                activity?.application?.unregisterActivityLifecycleCallbacks(callbacks)
                activityLifecycleCallbacks = null
            }

            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private var isManagingLifecycle = false
    private var isManagingSavedState = false
    private var isManagingViewModelStore = false
    private val isManagingViewTree: Boolean
        get() = isManagingLifecycle || isManagingSavedState || isManagingViewModelStore

    private var lifecycleOwner: LifecycleOwner? = null
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            this@CompatComposeView.onDestroy()
        }
    }
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleOwner?.lifecycle
            // Defaulting to a lifecycle in INITIALIZED state if none is set.
            ?: object : Lifecycle() {
                override val currentState: State
                    get() = State.INITIALIZED

                override fun addObserver(observer: LifecycleObserver) {
                    Logger.e("CompatComposeView: Attempted to add a LifecycleObserver when no LifecycleOwner is set.")
                }

                override fun removeObserver(observer: LifecycleObserver) {
                    Logger.e("CompatComposeView: Attempted to remove LifecycleObserver when no LifecycleOwner is set.")
                }
            }
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
        // This is just a safety measure in case the View is re-attached after being detached without having
        // been destroyed yet.
        lifecycleOwner?.let { lifecycleOwner ->
            if (lifecycleOwner is ViewLifecycleOwner) {
                Logger.w("Attaching a previously-detached view to a window. Resetting state")
                lifecycleOwner.destroy()
                onDestroy()
            }
        }
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

    /**
     * Provide a manual destroy signal.
     */
    protected fun destroy() {
        Logger.d("CompatComposeView: Destroying lifecycle owner since destroy() was called.")
        (lifecycleOwner as? ViewLifecycleOwner)?.destroy()
    }

    /**
     * Called when our lifecycle moves to `DESTROYED`, regardless of whether we are managing the lifecycle or not.
     */
    private fun onDestroy() {
        if (isManagingViewModelStore) viewModelStore.clear()
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
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
        val viewTreeLifecycleOwner = windowRoot.findViewTreeLifecycleOwner()
        if (lifecycleOwner == null) {
            lifecycleOwner = viewTreeLifecycleOwner ?: ViewLifecycleOwner(activity = context.getActivity())
            lifecycle.addObserver(lifecycleObserver)
        }
        if (viewTreeLifecycleOwner == null) {
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
