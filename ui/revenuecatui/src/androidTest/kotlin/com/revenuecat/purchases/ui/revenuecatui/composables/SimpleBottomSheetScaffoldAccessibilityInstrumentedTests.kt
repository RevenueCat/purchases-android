package com.revenuecat.purchases.ui.revenuecatui.composables

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
internal class SimpleBottomSheetScaffoldAccessibilityInstrumentedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
                        Text("Behind the sheet")
                    }
                },
            )
        }
        val behindNode = onNodeWithText("Behind the sheet", useUnmergedTree = true).fetchSemanticsNode()
        val root: RootForTest = requireNotNull(behindNode.root)
        assertThat(root.isExposed(behindNode.id)).isTrue()

        runOnUiThread {
            sheetState.show(backgroundBlur = backgroundBlur, content = { Text("In the sheet") })
        }
        waitForIdle()
        assertThat(root.isExposed(behindNode.id)).isFalse()

        runOnUiThread { sheetState.hide() }
        waitForIdle()
        assertThat(root.isExposed(behindNode.id)).isTrue()
    }

    // Accessibility is only forced on while querying. Leaving it on makes the sheet's state changes
    // send accessibility events, which throw when no accessibility service is actually running.
    private fun RootForTest.isExposed(semanticsId: Int): Boolean = composeTestRule.runOnIdle {
        forceAccessibilityForTesting(true)
        val exposed = composeTestRule.activity.window.decorView.findAccessibilityNodeProvider()
            .createAccessibilityNodeInfo(semanticsId) != null
        forceAccessibilityForTesting(false)
        exposed
    }

    private fun View.findAccessibilityNodeProvider(): AccessibilityNodeProvider =
        findAccessibilityNodeProviderOrNull() ?: error("Expected the Compose view to provide accessibility nodes")

    private fun View.findAccessibilityNodeProviderOrNull(): AccessibilityNodeProvider? =
        accessibilityNodeProvider
            ?: (this as? ViewGroup)?.let { group ->
                (0 until group.childCount).firstNotNullOfOrNull { index ->
                    group.getChildAt(index).findAccessibilityNodeProviderOrNull()
                }
            }
}
