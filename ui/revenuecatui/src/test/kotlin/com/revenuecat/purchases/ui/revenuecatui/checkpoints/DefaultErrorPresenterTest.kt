package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter.Completion.Result
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
class DefaultErrorPresenterTest {

    private lateinit var controller: ActivityController<Activity>
    private lateinit var mockPurchases: Purchases
    private lateinit var presenter: DefaultErrorPresenter

    private val messages = mutableListOf<String>()
    private var acknowledge: (() -> Unit)? = null
    private val results = mutableListOf<Result>()
    private val completion = ErrorPresenter.Completion { results += it }

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
        assertThat(results).isEmpty()
    }

    @Test
    fun `a purchase error shows its code's description`() {
        present(params(error = PurchasesError(PurchasesErrorCode.StoreProblemError, "raw store detail")))

        assertThat(messages).containsExactly(PurchasesErrorCode.StoreProblemError.description)
    }

    @Test
    fun `a presentation error shows the paywall's reason`() {
        present(
            params(
                source = ErrorPresenter.Source.PRESENTATION,
                flowCanContinue = false,
                error = PurchasesError(PurchasesErrorCode.UnknownError, "No packages available"),
            ),
        )

        assertThat(messages).containsExactly("No packages available")
    }

    @Test
    fun `presenting without a started activity throws and shows nothing`() {
        every { mockPurchases.currentActivity } returns null
        val dialogsShown = ShadowDialog.getShownDialogs().size

        assertThatThrownBy { present() }
            .isInstanceOf(PurchasesException::class.java)
            .matches { (it as PurchasesException).error.code == PurchasesErrorCode.ConfigurationError }

        assertThat(ShadowDialog.getShownDialogs()).hasSize(dialogsShown)
        assertThat(results).isEmpty()
    }

    @Test
    fun `acknowledging a recoverable error reports Retry and takes the dialog down`() {
        present(params(flowCanContinue = true))

        acknowledge!!()

        assertThat(results).containsExactly(Result.Retry)
        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
    }

    @Test
    fun `acknowledging an unrecoverable error reports Continued`() {
        present(params(flowCanContinue = false))

        acknowledge!!()

        assertThat(results).containsExactly(Result.Continued)
    }

    @Test
    fun `only the first acknowledgement counts`() {
        present()

        acknowledge!!()
        acknowledge!!()

        assertThat(results).containsExactly(Result.Retry)
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
        assertThat(results).isEmpty()
        acknowledge!!()
        assertThat(results).containsExactly(Result.Retry)
    }

    @Test
    fun `the host activity finishing reports Retry`() {
        present()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertThat(results).containsExactly(Result.Retry)
        assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
    }

    @Test
    fun `a second error replaces the first, which becomes a Retry`() {
        present()
        val firstDialog = ShadowDialog.getLatestDialog()
        val secondResults = mutableListOf<Result>()

        presenter.present(params(flowCanContinue = false), ErrorPresenter.Completion { secondResults += it })

        assertThat(results).containsExactly(Result.Retry)
        assertThat(firstDialog.isShowing).isFalse
        assertThat(ShadowDialog.getLatestDialog().isShowing).isTrue
        acknowledge!!()
        assertThat(secondResults).containsExactly(Result.Continued)
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
        source: ErrorPresenter.Source = ErrorPresenter.Source.PURCHASE,
        flowCanContinue: Boolean = true,
        error: PurchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom"),
    ) = ErrorPresenter.Params(error, "test_checkpoint", emptyMap(), source, flowCanContinue)
}
