package com.revenuecat.purchases

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class SimulatedStoreErrorDialogActivityTest {

    private val redactedApiKey = "test_redacted_api_key"

    @Test
    fun `show() launches activity with correct extras`() {
        // Arrange
        val contextMock = mockk<Context>(relaxed = true)
        val intentSlot = slot<Intent>()
        every { contextMock.startActivity(capture(intentSlot)) } returns Unit

        // Act
        SimulatedStoreErrorDialogActivity.show(contextMock, redactedApiKey)

        // Assert
        verify { contextMock.startActivity(any()) }
        val capturedIntent = intentSlot.captured
        assertThat(capturedIntent.component?.className).isEqualTo(SimulatedStoreErrorDialogActivity::class.java.name)
        assertThat(capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
        assertThat(capturedIntent.getStringExtra("redactedApiKey")).isEqualTo(redactedApiKey)
    }

    @Test
    fun `onBackPressed crashes app`() {
        // Arrange
        var exceptionThrown = false
        var thrownException: PurchasesException? = null

        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), SimulatedStoreErrorDialogActivity::class.java).apply {
            putExtra("redactedApiKey", redactedApiKey)
        }

        try {
            launchActivity<SimulatedStoreErrorDialogActivity>(intent).use { scenario ->
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
            "Test Store API key used in release build: $redactedApiKey. Please configure the " +
                "Play Store/Amazon app on the RevenueCat dashboard and use its corresponding API key " +
                "before releasing. Visit https://rev.cat/sdk-test-store to learn more."
        )
    }

    @Test
    fun `moving activity to paused state crashes app`() {
        // Arrange
        var exceptionThrown = false
        var thrownException: PurchasesException? = null

        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), SimulatedStoreErrorDialogActivity::class.java).apply {
            putExtra("redactedApiKey", redactedApiKey)
        }

        try {
            launchActivity<SimulatedStoreErrorDialogActivity>(intent).use { scenario ->
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
            "Test Store API key used in release build: $redactedApiKey. Please configure the " +
            "Play Store/Amazon app on the RevenueCat dashboard and use its corresponding API key " +
            "before releasing. Visit https://rev.cat/sdk-test-store to learn more."
        )
    }

    @Test
    fun `activity finishes gracefully when not launched through SDK`() {
        // Arrange - launch without the redactedApiKey extra (simulating Google automated testing)
        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), SimulatedStoreErrorDialogActivity::class.java)

        // Act & Assert - should not throw, activity should call finish()
        launchActivity<SimulatedStoreErrorDialogActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            // Moving to STARTED triggers onPause(), which calls crashApp()
            // Without SDK extras, crashApp() should call finish() instead of throwing
            scenario.moveToState(Lifecycle.State.STARTED)
            // If we get here without an exception, the activity finished gracefully
            scenario.onActivity { activity ->
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

}
