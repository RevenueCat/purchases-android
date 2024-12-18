package com.revenuecat.purchases.ui.revenuecatui.components

import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.localeChangingTest
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.net.URL

@RunWith(Enclosed::class)
internal class LoadedPaywallComponentsLocaleTests {
    private companion object {
        private val localizationKey = LocalizationKey("hello-world")
        
        const val EXPECTED_TEXT_EN = "Hello, world!"
        const val EXPECTED_TEXT_NL = "Hallo, wereld!"

        val paywallComponents = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(
                                text = localizationKey,
                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
                            )
                        )
                    ),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(
                    localizationKey to LocalizationData.Text(EXPECTED_TEXT_EN),
                ),
                LocaleId("nl_NL") to mapOf(
                    localizationKey to LocalizationData.Text(EXPECTED_TEXT_NL),
                ),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = paywallComponents,
        )
    }

    @RunWith(AndroidJUnit4::class)
    class WithActivityRecreationTests {

        @get:Rule(order = 1)
        val addActivityToRobolectricRule = object : TestWatcher() {
            override fun starting(description: Description?) {
                super.starting(description)
                val appContext: Application = getApplicationContext()
                val activityInfo = ActivityInfo().apply {
                    name = TestActivity::class.java.name
                    packageName = appContext.packageName
                }
                shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
            }
        }

        @get:Rule(order = 2)
        internal val composeTestRule = createAndroidComposeRule<TestActivity>()

        @Test
        fun `Should propagate locale changes after Activity recreation`(): Unit = with(composeTestRule) {
            // Assert that the locale is en_US at first.
            onNodeWithText(EXPECTED_TEXT_EN)
                .assertIsDisplayed()
            // This changes the locale to nl_NL and restarts the Activity.
            RuntimeEnvironment.setQualifiers("+nl-rNL")
            // Assert that the nl_NL text is now displayed.
            onNodeWithText(EXPECTED_TEXT_NL)
                .assertIsDisplayed()
        }

        class TestActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                val state = PaywallState.Loaded.Components(offering, paywallComponents)
                setContent { LoadedPaywallComponents(state = state) }
            }
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WithoutActivityRecreationTests {

        @get:Rule
        val composeTestRule = createComposeRule()
        
        @Test
        fun `Should propagate locale changes without Activity recreation`(): Unit = with(composeTestRule) {
            localeChangingTest(
                arrange = { PaywallState.Loaded.Components(offering, paywallComponents) },
                act = { state -> LoadedPaywallComponents(state = state) },
                assert = { localeController ->
                    localeController.setLocale("en-US")
                    onNodeWithText(EXPECTED_TEXT_EN)
                        .assertIsDisplayed()

                    localeController.setLocale("nl-NL")
                    onNodeWithText(EXPECTED_TEXT_NL)
                        .assertIsDisplayed()
                }
            )
            
        }
    }
}
