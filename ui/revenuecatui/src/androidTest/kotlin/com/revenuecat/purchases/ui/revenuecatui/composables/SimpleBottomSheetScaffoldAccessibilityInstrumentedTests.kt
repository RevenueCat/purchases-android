package com.revenuecat.purchases.ui.revenuecatui.composables

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class SimpleBottomSheetScaffoldAccessibilityInstrumentedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private var originalServiceFlags = 0

    // Without these, API 32 leaves the Compose text out of the window that UiAutomation reports.
    @Before
    fun includeAllViewsInTheAccessibilityTree() {
        uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply {
            originalServiceFlags = flags
            flags = flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    @After
    fun restoreServiceFlags() {
        uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply { flags = originalServiceFlags }
    }

    @Test
    fun platformAccessibilityDoesNotExposeContentBehindAnOpenSheetWithoutBlur() {
        assertContentBehindTheSheetIsHidden(backgroundBlur = false)
    }

    @Test
    fun platformAccessibilityDoesNotExposeContentBehindAnOpenSheetWithBlur() {
        assertContentBehindTheSheetIsHidden(backgroundBlur = true)
    }

    private fun assertContentBehindTheSheetIsHidden(backgroundBlur: Boolean): Unit = with(composeTestRule) {
        val sheetState = SimpleSheetState()
        setContent {
            SimpleBottomSheetScaffold(
                sheetState = sheetState,
                modifier = Modifier.fillMaxSize(),
                content = {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(BEHIND_THE_SHEET)
                    }
                },
            )
        }
        awaitAccessibilityTexts { BEHIND_THE_SHEET in it }

        runOnUiThread {
            sheetState.show(backgroundBlur = backgroundBlur, content = { Text(IN_THE_SHEET) })
        }
        awaitAccessibilityTexts { IN_THE_SHEET in it && BEHIND_THE_SHEET !in it }

        runOnUiThread { sheetState.hide() }
        awaitAccessibilityTexts { BEHIND_THE_SHEET in it }
    }

    // Queries through UiAutomation, which connects as a real accessibility service like TalkBack does.
    private fun awaitAccessibilityTexts(condition: (List<String>) -> Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) { condition(accessibilityTexts()) }
    }

    private fun accessibilityTexts(): List<String> =
        uiAutomation.rootInActiveWindow?.let { collectTexts(it) }.orEmpty()

    private fun collectTexts(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString()) + (0 until node.childCount).flatMap { index ->
            node.getChild(index)?.let { collectTexts(it) }.orEmpty()
        }

    private companion object {
        const val BEHIND_THE_SHEET = "Behind the sheet"
        const val IN_THE_SHEET = "In the sheet"
    }
}
