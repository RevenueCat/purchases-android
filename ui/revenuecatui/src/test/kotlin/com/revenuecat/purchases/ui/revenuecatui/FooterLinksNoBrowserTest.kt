package com.revenuecat.purchases.ui.revenuecatui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class FooterLinksNoBrowserTest {

    @get:Rule(order = 1)
    val addActivityToRobolectricRule = object : TestWatcher() {
        override fun starting(description: Description?) {
            super.starting(description)
            val appContext: Application = getApplicationContext()
            val activityInfo = ActivityInfo().apply {
                name = IntentResolvingActivity::class.java.name
                packageName = appContext.packageName
            }
            shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
        }
    }

    @get:Rule(order = 2)
    internal val composeTestRule = createAndroidComposeRule<IntentResolvingActivity>()

    private lateinit var viewModel: MockViewModel

    @Before
    fun setUp() {
        viewModel = MockViewModel(offering = TestData.template2Offering, shouldErrorOnUnsupportedMethods = false)
    }

    @Test
    fun `Clicking Terms without a browser should not crash and show a toast`() {
        // Arrange
        assertNoBrowser()
        setUpUI()

        // Act
        composeTestRule
            .onNodeWithText("Terms and conditions")
            .performClick()

        // Assert
        val expectedToast = getString(R.string.no_browser_cannot_open_link)
        assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
        assertThat(ShadowToast.showedToast(expectedToast)).isTrue()
    }

    @Test
    fun `Clicking Privacy without a browser should not crash and show a toast`() {
        // Arrange
        assertNoBrowser()
        setUpUI()

        // Act
        composeTestRule
            .onNodeWithText("Privacy policy")
            .performClick()

        // Assert
        val expectedToast = getString(R.string.no_browser_cannot_open_link)
        assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
        assertThat(ShadowToast.showedToast(expectedToast)).isTrue()
    }

    @Test
    fun `Clicking Terms with a browser should not show a toast`() {
        // Arrange
        installBrowser()
        assertBrowser()
        setUpUI()

        // Act
        composeTestRule
            .onNodeWithText("Terms and conditions")
            .performClick()

        // Assert
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    }

    @Test
    fun `Clicking Privacy with a browser should not show a toast`() {
        // Arrange
        installBrowser()
        assertBrowser()
        setUpUI()

        // Act
        composeTestRule
            .onNodeWithText("Privacy policy")
            .performClick()

        // Assert
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    }

    private fun setUpUI() {
        composeTestRule.setContent {
            InternalPaywall(
                options = PaywallOptions.Builder { Assert.fail("Should not be dismissed") }.build(),
                viewModel = viewModel,
            )
        }
    }

    /**
     * Asserts that our environment has no browser installed.
     */
    private fun assertNoBrowser() {
        val implicitBrowserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"))

        assertThatExceptionOfType(ActivityNotFoundException::class.java)
            .isThrownBy { composeTestRule.activity.startActivity(implicitBrowserIntent) }
    }

    /**
     * Asserts that our environment has a browser installed.
     */
    private fun assertBrowser() {
        val implicitBrowserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"))

        assertThatNoException()
            .isThrownBy { composeTestRule.activity.startActivity(implicitBrowserIntent) }
    }

    private fun getString(@StringRes resId: Int) =
        composeTestRule.activity.getString(resId)

    private fun installBrowser() {
        shadowOf(getApplicationContext<Context>().packageManager).addBrowser()
    }

    private fun ShadowPackageManager.addBrowser() {
        val activityInfo = ActivityInfo().apply {
            name = "browser"
            packageName = "com.browser"
        }
        val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
        val intentFilter = IntentFilter(Intent.ACTION_VIEW)
            .apply { addDataScheme("https") }
            .apply { addDataScheme("http") }
            .apply { addCategory(Intent.CATEGORY_DEFAULT) }

        addOrUpdateActivity(activityInfo)
        addIntentFilterForActivity(componentName, intentFilter)
    }
}

/**
 * An Activity that actually resolves Intents. Normally Robolectric doesn't do this.
 */
internal class IntentResolvingActivity : ComponentActivity() {

    override fun startActivity(intent: Intent) {
        val packageManager = getApplicationContext<Context>().packageManager
        if (intent.resolveActivity(packageManager) != null) super.startActivity(intent)
        else throw ActivityNotFoundException("No Activity to handle Intent: $intent")
    }
}
