package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownCodeTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `underline tags inside inline code are displayed literally`(): Unit = with(composeTestRule) {
        setContent {
            Markdown(text = "`<u>code</u>`")
        }

        val text = annotatedText("<u>code</u>")

        assertThat(text.underlinedSubstrings()).isEmpty()
    }

    @Test
    fun `inline code is excluded from surrounding underline`(): Unit = with(composeTestRule) {
        setContent {
            Markdown(text = "<u>before `code` after</u>")
        }

        val text = annotatedText("before code after")

        assertThat(text.underlinedSubstrings()).containsExactly("before ", " after")
    }

    private fun ComposeContentTestRule.annotatedText(text: String): AnnotatedString =
        onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .single()

    private fun AnnotatedString.underlinedSubstrings(): List<String> =
        spanStyles
            .filter { it.item.textDecoration == TextDecoration.Underline }
            .map { text.substring(it.start, it.end) }
}
