@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
class CheckpointWorkflowPresenterTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var controller: ActivityController<Activity>
    private lateinit var mockPurchases: Purchases
    private lateinit var manager: CheckpointsManager

    private val presentedCallIds = mutableListOf<String>()
    private var lastOptions: PaywallOptions? = null
    private var result: CheckpointResult? = null
    private var contentFactory: (Activity) -> View = { activity -> View(activity) }
    private val contentViews = mutableListOf<View>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        controller = Robolectric.buildActivity(Activity::class.java).setup()
        mockPurchases = mockk {
            every { currentActivity } answers { controller.get() }
            coEvery { resolveCheckpoint(any(), any()) } returns
                CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk())
        }
        manager = CheckpointsManager { callId, manager ->
            presentedCallIds += callId
            CheckpointWorkflowPresenter(callId, manager) { activity, options ->
                lastOptions = options
                contentFactory(activity).also { contentViews += it }
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `presenting a checkpoint workflow shows a dialog window over the current activity`() {
        launchCheckpoint()

        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog).isNotNull
        assertThat(dialog.isShowing).isTrue
        assertThat(result).isNull()
    }

    @Test
    fun `showing a stale callId does not present and does not disturb the live call`() {
        launchCheckpoint()
        val liveDialog = ShadowDialog.getLatestDialog()

        CheckpointWorkflowPresenter("call-id-from-a-previous-presentation", manager) { activity, _ ->
            View(activity)
        }.show(controller.get())

        assertThat(ShadowDialog.getLatestDialog()).isSameAs(liveDialog)
        assertThat(liveDialog.isShowing).isTrue
        assertThat(manager.presentation(currentCallId())).isNotNull
    }

    @Test
    fun `a dismiss request completes the call as Dismissed`() {
        launchCheckpoint()

        lastOptions!!.dismissRequest()

        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
        assertThat(paywallOutcome()).isEqualTo(CheckpointPaywallOutcome.Dismissed)
    }

    @Test
    fun `a configuration change re-presents over the recreated activity`() {
        launchCheckpoint()
        val firstDialog = ShadowDialog.getLatestDialog()

        controller.recreate()

        val secondDialog = ShadowDialog.getLatestDialog()
        assertThat(firstDialog.isShowing).isFalse
        assertThat(secondDialog).isNotSameAs(firstDialog)
        assertThat(secondDialog.isShowing).isTrue
        assertThat(result).isNull()
    }

    @Test
    fun `an outcome recorded before a configuration change is the one delivered after it`() {
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        launchCheckpoint()
        manager.recordOutcome(currentCallId(), CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))

        controller.recreate()
        lastOptions!!.dismissRequest()

        assertThat(paywallOutcome()).isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `the host activity finishing completes the call with the recorded outcome`() {
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        launchCheckpoint()
        manager.recordOutcome(currentCallId(), CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
        val dialog = ShadowDialog.getLatestDialog()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertThat(dialog.isShowing).isFalse
        assertThat(paywallOutcome()).isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `no re-present happens on activities started after the host finished`() {
        launchCheckpoint()
        controller.get().finish()
        controller.pause().stop().destroy()
        val dialogsShown = ShadowDialog.getShownDialogs().size

        Robolectric.buildActivity(Activity::class.java).setup()

        assertThat(ShadowDialog.getShownDialogs()).hasSize(dialogsShown)
    }

    @Test
    fun `cancelling the caller dismisses the window and stops observing the host`() {
        val call = launchCheckpoint()

        call.cancel()

        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
        val dialogsShown = ShadowDialog.getShownDialogs().size
        controller.recreate()
        assertThat(ShadowDialog.getShownDialogs()).hasSize(dialogsShown)
    }

    @Test
    fun `an external dismissal completes the call with the recorded outcome`() {
        launchCheckpoint()

        ShadowDialog.getLatestDialog().dismiss()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(paywallOutcome()).isEqualTo(CheckpointPaywallOutcome.Dismissed)
    }

    @Test
    fun `a stale dismiss callback from before a configuration change does not take down the re-presented window`() {
        launchCheckpoint()

        // Dialog delivers the dismiss callback through a posted message: dismissing without idling leaves
        // that message in flight across the recreation, exactly like an external dismissal racing a rotation.
        ShadowDialog.getLatestDialog().dismiss()
        controller.recreate()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(ShadowDialog.getLatestDialog().isShowing).isTrue
        assertThat(result).isNull()
    }

    @Test
    fun `view state is restored in the re-presented window after a configuration change`() {
        contentFactory = { activity -> EditText(activity).apply { id = CONTENT_VIEW_ID } }
        launchCheckpoint()
        (contentViews.last() as EditText).setText("mid-workflow input")

        controller.recreate()

        assertThat((contentViews.last() as EditText).text.toString()).isEqualTo("mid-workflow input")
    }

    @Test
    fun `a failed re-present completes the call instead of hanging`() {
        val customerInfo = mockk<CustomerInfo>()
        launchCheckpoint()
        manager.recordOutcome(currentCallId(), CheckpointPaywallOutcome.Purchased(customerInfo))
        contentFactory = { throw IllegalStateException("content failed") }

        controller.recreate()

        assertThat(paywallOutcome()).isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo))
    }

    @Test
    fun `cancelling after a configuration change does not resurrect the workflow on a later recreation`() {
        val call = launchCheckpoint()
        controller.recreate()

        call.cancel()

        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
        val dialogsShown = ShadowDialog.getShownDialogs().size
        controller.recreate()
        assertThat(ShadowDialog.getShownDialogs()).hasSize(dialogsShown)
    }

    @Test
    @Config(sdk = [35])
    fun `the workflow window extends behind the system bars where edge to edge is the default`() {
        launchCheckpoint()

        val window = ShadowDialog.getLatestDialog().window!!
        val attributes = window.attributes
        assertThat(attributes.fitInsetsTypes).isEqualTo(0)
        assertThat(attributes.fitInsetsSides).isEqualTo(0)
        assertThat(attributes.layoutInDisplayCutoutMode)
            .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN).isNotEqualTo(0)
        @Suppress("DEPRECATION")
        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR).isNotEqualTo(0)
        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            .isNotEqualTo(0)
        assertThat(window.isNavigationBarContrastEnforced).isFalse
    }

    @Test
    @Config(sdk = [29])
    @Suppress("DEPRECATION")
    fun `the workflow window extends behind the system bars on legacy APIs`() {
        launchCheckpoint()

        val window = ShadowDialog.getLatestDialog().window!!
        val systemUiVisibility = window.decorView.systemUiVisibility
        assertThat(systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_STABLE).isNotEqualTo(0)
        assertThat(systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN).isNotEqualTo(0)
        assertThat(systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION).isNotEqualTo(0)
        assertThat(window.attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            .isNotEqualTo(0)
        assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
        assertThat(window.navigationBarColor).isEqualTo(Color.TRANSPARENT)
        assertThat(window.attributes.layoutInDisplayCutoutMode)
            .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
    }

    private fun launchCheckpoint(): Job = CoroutineScope(dispatcher).launch {
        result = manager.checkpoint(mockPurchases, "test_checkpoint", null)
    }

    private fun currentCallId(): String = presentedCallIds.last()

    private fun paywallOutcome(): CheckpointPaywallOutcome? =
        (result as? CheckpointResult.PaywallPresented)?.paywallOutcome

    private companion object {
        const val CONTENT_VIEW_ID = 4242
    }
}
