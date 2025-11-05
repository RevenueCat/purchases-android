package com.revenuecat.purchases

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestStoreErrorDialogActivityTest {

    @Test
    fun `show() launches activity with correct flags`() {
        // Arrange
        val contextMock = mockk<Context>(relaxed = true)
        val intentSlot = slot<Intent>()
        every { contextMock.startActivity(capture(intentSlot)) } returns Unit

        // Act
        TestStoreErrorDialogActivity.show(contextMock)

        // Assert
        verify { contextMock.startActivity(any()) }
        val capturedIntent = intentSlot.captured
        assertThat(capturedIntent.component?.className).isEqualTo(TestStoreErrorDialogActivity::class.java.name)
        assertThat(capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `onBackPressed crashes app`() {
        // Arrange
        var exceptionThrown = false
        var thrownException: PurchasesException? = null

        try {
            launchActivity<TestStoreErrorDialogActivity>().use { scenario ->
                scenario.moveToState(Lifecycle.State.CREATED)

                scenario.onActivity { activity ->
                    // Act
                    activity.onBackPressed()
                }
            }
        } catch (e: PurchasesException) {
            exceptionThrown = true
            thrownException = e
        }

        // Assert
        assertThat(exceptionThrown).isTrue()
        assertThat(thrownException?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(thrownException?.message).isEqualTo(
            "Test Store API key used in release build. Please configure the " +
                "Play Store app on the RevenueCat dashboard and use its corresponding Google API key " +
                "before releasing. Visit https://rev.cat/sdk-test-store to learn more."
        )
    }

    @Test
    fun `moving activity to paused state crashes app`() {
        // Arrange
        var exceptionThrown = false
        var thrownException: PurchasesException? = null

        try {
            launchActivity<TestStoreErrorDialogActivity>().use { scenario ->
                scenario.moveToState(Lifecycle.State.RESUMED)
                // Act
                // Moving from RESUMED to STARTED will trigger onPause()
                scenario.moveToState(Lifecycle.State.STARTED)
            }
        } catch (e: PurchasesException) {
            exceptionThrown = true
            thrownException = e
        }

        // Assert
        assertThat(exceptionThrown).isTrue()
        assertThat(thrownException?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(thrownException?.message).isEqualTo(
            "Test Store API key used in release build. Please configure the " +
            "Play Store app on the RevenueCat dashboard and use its corresponding Google API key " +
            "before releasing. Visit https://rev.cat/sdk-test-store to learn more."
        )
    }

}
