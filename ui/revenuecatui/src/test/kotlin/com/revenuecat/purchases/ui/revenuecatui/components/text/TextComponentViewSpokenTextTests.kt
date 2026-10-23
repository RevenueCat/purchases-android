package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TalkBack reads the spoken copy of a text ("$1.99 monthly") while the screen keeps the written one ("$1.99/mo").
 */
@RunWith(AndroidJUnit4::class)
class TextComponentViewSpokenTextTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val localeId = LocaleId("en_US")
    private val textKey = LocalizationKey("text")
    private val rcPackage = TestData.Packages.monthly

    @Test
    fun `price per period is spoken as words while the abbreviation is displayed`() {
        val displayed = setTextContent("{{ product.price_per_period_abbreviated }}")

        composeTestRule.onNodeWithContentDescription("${rcPackage.product.price.formatted} monthly").assertExists()
        composeTestRule.onNodeWithText(displayed, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `spoken text drops markdown syntax`() {
        setTextContent("**{{ product.price_per_period_abbreviated }}**. <u>Cancel</u> anytime.")

        composeTestRule.onNodeWithContentDescription("${rcPackage.product.price.formatted} monthly. Cancel anytime.")
            .assertExists()
    }

    @Test
    fun `text with links keeps its displayed text so the links stay reachable`() {
        val displayed = setTextContent("{{ product.price_per_period_abbreviated }}. See [terms](https://rev.cat/mo).")

        composeTestRule.onNodeWithText(displayed)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    /**
     * The spoken copy can't be used to look for links: its expansion can rewrite a link target that has no scheme,
     * like `myapp:plans/month`, into something that's no longer a link.
     */
    @Test
    fun `text with a link whose target reads like a period keeps its displayed text`() {
        val displayed = setTextContent("{{ product.price_per_period_abbreviated }}. See [plans](myapp:plans/month).")
            .replace("[plans](myapp:plans/month)", "plans")

        composeTestRule.onNodeWithText(displayed)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun `text without abbreviations has no separate spoken text`() {
        val displayed = setTextContent("Unlock everything")

        composeTestRule.onNodeWithText(displayed)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    /**
     * Returns the displayed text.
     */
    private fun setTextContent(text: String): String {
        val localizations = nonEmptyMapOf(localeId to nonEmptyMapOf(textKey to LocalizationData.Text(text)))
        val component = TextComponent(
            text = textKey,
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            components = listOf(component),
            packages = listOf(rcPackage),
        )
        state.update(selectedPackageUniqueId = rcPackage.identifier)
        val style = StyleFactory(localizations = localizations).create(component).getOrThrow().componentStyle
            as TextComponentStyle

        composeTestRule.setContent { TextComponentView(style = style, state = state) }

        return text
            .replace("{{ product.price_per_period_abbreviated }}", "${rcPackage.product.price.formatted}/mo")
            .replace("[terms](https://rev.cat/mo)", "terms")
    }
}
