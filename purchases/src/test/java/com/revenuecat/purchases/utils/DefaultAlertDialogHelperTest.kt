package com.revenuecat.purchases.utils

import android.app.Activity
import android.content.DialogInterface
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DefaultAlertDialogHelperTest {

    private lateinit var activity: Activity
    private lateinit var helper: DefaultAlertDialogHelper

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        helper = DefaultAlertDialogHelper()
    }

    @Test
    fun `cancelling dialog triggers neutral callback`() {
        var neutralCalled = false

        helper.showDialog(
            activity = activity,
            title = "Title",
            message = "Message",
            positiveButtonText = "OK",
            negativeButtonText = "Fail",
            neutralButtonText = "Cancel",
            onPositiveButtonClicked = { },
            onNegativeButtonClicked = { },
            onNeutralButtonClicked = { neutralCalled = true },
        )
        shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertThat(dialog).isNotNull()

        dialog.cancel()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(neutralCalled).isTrue()
    }

    @Test
    fun `positive button triggers only positive callback`() {
        var positiveCalled = false
        var negativeCalled = false
        var neutralCalled = false

        helper.showDialog(
            activity = activity,
            title = "Title",
            message = "Message",
            positiveButtonText = "OK",
            negativeButtonText = "Fail",
            neutralButtonText = "Cancel",
            onPositiveButtonClicked = { positiveCalled = true },
            onNegativeButtonClicked = { negativeCalled = true },
            onNeutralButtonClicked = { neutralCalled = true },
        )
        shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(positiveCalled).isTrue()
        assertThat(negativeCalled).isFalse()
        assertThat(neutralCalled).isFalse()
    }

    @Test
    fun `negative button triggers only negative callback`() {
        var positiveCalled = false
        var negativeCalled = false
        var neutralCalled = false

        helper.showDialog(
            activity = activity,
            title = "Title",
            message = "Message",
            positiveButtonText = "OK",
            negativeButtonText = "Fail",
            neutralButtonText = "Cancel",
            onPositiveButtonClicked = { positiveCalled = true },
            onNegativeButtonClicked = { negativeCalled = true },
            onNeutralButtonClicked = { neutralCalled = true },
        )
        shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(positiveCalled).isFalse()
        assertThat(negativeCalled).isTrue()
        assertThat(neutralCalled).isFalse()
    }

    @Test
    fun `neutral button triggers only neutral callback`() {
        var positiveCalled = false
        var negativeCalled = false
        var neutralCalled = false

        helper.showDialog(
            activity = activity,
            title = "Title",
            message = "Message",
            positiveButtonText = "OK",
            negativeButtonText = "Fail",
            neutralButtonText = "Cancel",
            onPositiveButtonClicked = { positiveCalled = true },
            onNegativeButtonClicked = { negativeCalled = true },
            onNeutralButtonClicked = { neutralCalled = true },
        )
        shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(positiveCalled).isFalse()
        assertThat(negativeCalled).isFalse()
        assertThat(neutralCalled).isTrue()
    }
}
