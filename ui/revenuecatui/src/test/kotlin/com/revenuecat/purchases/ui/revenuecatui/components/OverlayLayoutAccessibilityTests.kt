package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class OverlayLayoutAccessibilityTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `sets header traversal priority without overriding footer order`(): Unit = with(composeTestRule) {
        val state = FakePaywallState()
        setContent {
            PaywallComponentsScaffold(
                state = state,
                modifier = Modifier.fillMaxSize(),
                background = null,
                headerContent = {
                    Box(
                        Modifier
                            .testTag("header")
                            .semantics { contentDescription = "Header" }
                            .fillMaxWidth()
                            .height(40.dp),
                    )
                },
                footerContent = {
                    Box(
                        Modifier
                            .testTag("footer")
                            .semantics { contentDescription = "Footer" }
                            .fillMaxWidth()
                            .height(60.dp),
                    )
                },
            ) {
                Box(
                    Modifier
                        .testTag("body")
                        .semantics { contentDescription = "Body" }
                        .fillMaxSize(),
                )
            }
        }

        val headerGroup = onNodeWithTag("header", useUnmergedTree = true)
            .onParent()
            .assertTraversalGroup(index = -1f)
        val bodyParent = onNodeWithTag("body", useUnmergedTree = true).onParent()
        val footerParent = onNodeWithTag("footer", useUnmergedTree = true).onParent()
        val overlayGroup = headerGroup.onParent()
        overlayGroup.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true),
        )
        val overlayId = overlayGroup.fetchSemanticsNode().id
        assertThat(bodyParent.fetchSemanticsNode().id).isEqualTo(overlayId)
        assertThat(footerParent.fetchSemanticsNode().id).isEqualTo(overlayId)
        assertThat(footerParent.fetchSemanticsNode().config.contains(SemanticsProperties.TraversalIndex)).isFalse()
    }

    private fun SemanticsNodeInteraction.assertTraversalGroup(index: Float): SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, index))
}
