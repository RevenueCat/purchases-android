package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.os.Looper
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
class DefaultErrorPresenterTest {

    private lateinit var controller: ActivityController<Activity>
    private lateinit var mockPurchases: Purchases
    private lateinit var presenter: DefaultErrorPresenter

    private val messages = mutableListOf<String>()
    private var acknowledge: (() -> Unit)? = null
    private var completions = 0
    private val completion = ErrorPresenter.Completion { completions++ }

    @Before
    fun setup() {
        controller = Robolectric.buildActivity(Activity::class.java).setup()
        mockPurchases = mockk { every { currentActivity } answers { controller.get() } }
        presenter = DefaultErrorPresenter(mockPurchases) { activity, message, onAcknowledged ->
            messages += message
            acknowledge = onAcknowledged
            View(activity)
        }
    }

    @Test
    fun `presenting shows a dialog over the current activity`() {
        present()

        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog).isNotNull
        assertThat(dialog.isShowing).isTrue
        assertThat(completions).isZero
    }

    @Test
    fun `an error during the flow shows its code's description`() {
        present(params(error = PurchasesError(PurchasesErrorCode.StoreProblemError, "raw store detail")))

        assertThat(messages).containsExactly(PurchasesErrorCode.StoreProblemError.description)
    }

    @Test
    fun `an error before the flow shows its reason`() {
        present(
            params(
                kind = CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN,
                error = PurchasesError(PurchasesErrorCode.UnknownError, "No packages available"),
            ),
        )

        assertThat(messages).containsExactly("No packages available")
    }

    @Test
    fun `an error after the flow shows its reason`() {
        present(
            params(
                kind = CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED,
                error = PurchasesError(PurchasesErrorCode.NetworkError, "Request timed out"),
            ),
        )

        assertThat(messages).containsExactly("Request timed out")
    }

    @Test
    fun `presenting without a started activity throws and shows nothing`() {
        every { mockPurchases.currentActivity } returns null
        val dialogsShown = ShadowDialog.getShownDialogs().size

        assertThatThrownBy { present() }
            .isInstanceOf(PurchasesException::class.java)
            .matches { (it as PurchasesException).error.code == PurchasesErrorCode.ConfigurationError }

        assertThat(ShadowDialog.getShownDialogs()).hasSize(dialogsShown)
        assertThat(completions).isZero
    }

    @Test
    fun `acknowledging the error completes and takes the dialog down`() {
        present()

        acknowledge!!()

        assertThat(completions).isEqualTo(1)
        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
    }

    @Test
    fun `cancelling the host window acknowledges the error instead of leaving it on screen`() {
        present()

        ShadowDialog.getLatestDialog().cancel()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(completions).isEqualTo(1)
        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
    }

    @Test
    fun `only the first acknowledgement counts`() {
        present()

        acknowledge!!()
        acknowledge!!()

        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a configuration change shows the dialog again over the recreated activity`() {
        present()
        val firstDialog = ShadowDialog.getLatestDialog()

        controller.recreate()

        val secondDialog = ShadowDialog.getLatestDialog()
        assertThat(secondDialog).isNotSameAs(firstDialog)
        assertThat(firstDialog.isShowing).isFalse
        assertThat(secondDialog.isShowing).isTrue
        assertThat(completions).isZero
        acknowledge!!()
        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `the host activity finishing completes`() {
        present()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertThat(completions).isEqualTo(1)
        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
    }

    @Test
    fun `a second error replaces the first, which completes`() {
        present()
        val firstDialog = ShadowDialog.getLatestDialog()
        var secondCompletions = 0

        presenter.present(
            params(kind = CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN),
            ErrorPresenter.Completion { secondCompletions++ },
        )

        assertThat(completions).isEqualTo(1)
        assertThat(firstDialog.isShowing).isFalse
        assertThat(ShadowDialog.getLatestDialog().isShowing).isTrue
        acknowledge!!()
        assertThat(secondCompletions).isEqualTo(1)
        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `no dialog is shown on activities started after the presentation finished`() {
        present()
        acknowledge!!()
        val dialogsShown = ShadowDialog.getShownDialogs().size

        controller.recreate()

        assertThat(ShadowDialog.getShownDialogs()).hasSize(dialogsShown)
    }

    private fun present(params: ErrorPresenter.Params = params()) = presenter.present(params, completion)

    private fun params(
        kind: CheckpointFailureKind = CheckpointFailureKind.FLOW_ACTION_FAILED,
        error: PurchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom"),
    ) = ErrorPresenter.Params(error, "test_checkpoint", emptyMap(), kind)
}
