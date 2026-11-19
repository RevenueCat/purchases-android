package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownBlockTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `thematic break lines are displayed as typed`() {
        val lines = listOf("---", "***", "___", "- - - - -", "* * *")
        composeTestRule.setContent {
            lines.forEach { Markdown(text = it) }
        }

        lines.forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `thematic break between paragraphs is displayed as typed`() {
        composeTestRule.setContent {
            Markdown(text = "Before\n\n---\n\nAfter")
        }

        listOf("Before", "---", "After").forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `dash line under a text is displayed as typed`() {
        composeTestRule.setContent {
            Markdown(text = "Price\n---")
        }

        composeTestRule.onNodeWithText("Price\n---").assertIsDisplayed()
    }

    @Test
    fun `paragraphs in a quote are displayed on separate lines`() {
        composeTestRule.setContent {
            Markdown(text = "> Quote\n>\n> ***\n>\n> More")
        }

        composeTestRule.onNodeWithText("Quote\n***\nMore").assertIsDisplayed()
    }

    @Test
    fun `indented text is displayed as typed`() {
        composeTestRule.setContent {
            Markdown(text = "    Indented")
        }

        composeTestRule.onNodeWithText("    Indented").assertIsDisplayed()
    }

    @Test
    fun `html block is displayed as typed`() {
        composeTestRule.setContent {
            Markdown(text = "<div>\nHtml\n</div>")
        }

        composeTestRule.onNodeWithText("<div>\nHtml\n</div>").assertIsDisplayed()
    }

    @Test
    fun `heading is still displayed without its marker`() {
        composeTestRule.setContent {
            Markdown(text = "# Title")
        }

        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("#", substring = true).assertCountEquals(0)
    }

    @Test
    fun `list items are still displayed one per line`() {
        composeTestRule.setContent {
            Markdown(text = "- One\n- Two\n\n1. First\n2. Second")
        }

        listOf("- One", "- Two", "1. First", "2. Second").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
    }
}
