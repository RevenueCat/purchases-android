package com.revenuecat.purchases.ui.revenuecatui.components.button

import android.os.LocaleList
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import kotlinx.coroutines.CompletableDeferred
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class ButtonComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `onClick ignores further clicks until processing current click is done`() {
        var actionHandleCalledCount = 0
        val completable = CompletableDeferred<Unit>()

        composeTestRule.setContent {
            val style = ButtonComponentStyle(
                stackComponentStyle = StackComponentStyle(
                    children = listOf(
                        TextComponentStyle(
                            texts = nonEmptyMapOf(LocaleId("en_US") to "Purchase"),
                            color = ColorStyles(
                                light = ColorStyle.Solid(Color.Black),
                            ),
                            fontSize = 15,
                            fontWeight = FontWeight.REGULAR.toFontWeight(),
                            fontSpec = null,
                            textAlign = CENTER.toTextAlign(),
                            horizontalAlignment = CENTER.toAlignment(),
                            backgroundColor = ColorStyles(
                                light = ColorStyle.Solid(Color.Yellow),
                            ),
                            visible = true,
                            size = Size(width = Fill, height = Fill),
                            padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                            margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0)
                                .toPaddingValues(),
                            rcPackage = null,
                            tabIndex = null,
                            variableLocalizations = nonEmptyMapOf(
                                LocaleId("en_US") to variableLocalizationKeysForEnUs()
                            ),
                            countdownDate = null,
                            countFrom = CountdownComponent.CountFrom.DAYS,
                            overrides = emptyList(),
                        ),
                    ),
                    dimension = Dimension.Vertical(alignment = CENTER, distribution = START),
                    visible = true,
                    size = Size(width = Fill, height = Fill),
                    spacing = 16.dp,
                    background = BackgroundStyles.Color(ColorStyles(ColorStyle.Solid(Color.Red))),
                    padding = PaddingValues(all = 16.dp),
                    margin = PaddingValues(all = 16.dp),
                    shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
                    border = BorderStyles(width = 2.dp, colors = ColorStyles(ColorStyle.Solid(Color.Blue))),
                    shadow = ShadowStyles(
                        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
                        radius = 10.dp,
                        x = 0.dp,
                        y = 3.dp,
                    ),
                    badge = null,
                    scrollOrientation = null,
                    rcPackage = null,
                    tabIndex = null,
                    countdownDate = null,
                    countFrom = CountdownComponent.CountFrom.DAYS,
                    overrides = emptyList(),
                ),
                action = ButtonComponentStyle.Action.PurchasePackage(rcPackage = null),
            )
            ButtonComponentView(
                style = style,
                state = FakePaywallState(TestData.Packages.annual),
                onClick = {
                    actionHandleCalledCount++
                    completable.await()
                }
            )
        }

        val purchaseButton = composeTestRule.onNodeWithText("Purchase")
        purchaseButton.assertExists()
        purchaseButton.performClick()
        purchaseButton.performClick()
        purchaseButton.performClick()

        assertThat(actionHandleCalledCount).isEqualTo(1)

        completable.complete(Unit)

        assertThat(actionHandleCalledCount).isEqualTo(1)

        purchaseButton.performClick()

        assertThat(actionHandleCalledCount).isEqualTo(2)
    }

    @Test
    fun `Should use the correct URL when the locale changes`(): Unit = with(composeTestRule) {
        val localeIdEnUs = LocaleId("en_US")
        val localeIdNlNl = LocaleId("nl_NL")
        val localizationKey = LocalizationKey("ineligible key")
        val expectedUrlEnUs = "expected"
        val expectedUrlNlNl = "verwacht"
        val component = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(
                destination = ButtonComponent.Destination.Url(
                    urlLid = localizationKey,
                    method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                )
            ),
            stack = StackComponent(
                components = listOf(
                    TextComponent(
                        text = localizationKey,
                        color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                    )
                )
            ),
        )
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                localizationKey to LocalizationData.Text(expectedUrlEnUs),
            ),
            localeIdNlNl to nonEmptyMapOf(
                localizationKey to LocalizationData.Text(expectedUrlNlNl),
            )
        )
        val styleFactory = StyleFactory(localizations = localizations)
        val style = styleFactory.create(component).getOrThrow().componentStyle as ButtonComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            packages = listOf(TestData.Packages.monthly),
            components = listOf(component)
        )

        // Act
        var clickedUrl: String? = null
        setContent {
            ButtonComponentView(
                style = style,
                onClick = { action ->
                    clickedUrl = action
                        .let { it as? PaywallAction.External.NavigateTo }
                        ?.let { it.destination as PaywallAction.External.NavigateTo.Destination.Url }
                        ?.url
                },
                state = state
            )
        }

        // Assert
        state.update(localeList = LocaleList(localeIdEnUs.toJavaLocale()))
        onNodeWithText(expectedUrlEnUs)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertThat(clickedUrl).isEqualTo(expectedUrlEnUs)

        state.update(localeList = LocaleList(localeIdNlNl.toJavaLocale()))
        onNodeWithText(expectedUrlNlNl)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        assertThat(clickedUrl).isEqualTo(expectedUrlNlNl)
    }

    @Test
    fun `If a purchase button is inside a package component, the button should be linked to that specific package`(): Unit =
        with(composeTestRule) {
            val ctaPurchaseAnnual = "purchase annual package"
            val ctaPurchaseMonthly = "purchase monthly package"
            val stackComponent = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = TestData.Packages.annual.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                PurchaseButtonComponent(
                                    stack = StackComponent(
                                        components = listOf(
                                            TextComponent(
                                                text = LocalizationKey("purchase-annual"),
                                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                            )
                                        )
                                    )
                                ),
                            )
                        )
                    ),
                    PackageComponent(
                        packageId = TestData.Packages.monthly.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                PurchaseButtonComponent(
                                    stack = StackComponent(
                                        components = listOf(
                                            TextComponent(
                                                text = LocalizationKey("purchase-monthly"),
                                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                            )
                                        )
                                    )
                                ),
                            )
                        )
                    ),
                ),
            )
            val packages = listOf(TestData.Packages.annual, TestData.Packages.monthly)
            val styleFactory = StyleFactory(
                localizations = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("purchase-annual") to LocalizationData.Text(ctaPurchaseAnnual),
                        LocalizationKey("purchase-monthly") to LocalizationData.Text(ctaPurchaseMonthly),
                    )
                ),
                offering = Offering(
                    identifier = "identifier",
                    serverDescription = "description",
                    metadata = emptyMap(),
                    availablePackages = packages,
                )
            )
            val style = styleFactory.create(stackComponent).getOrThrow().componentStyle as StackComponentStyle
            val state = FakePaywallState(packages = packages)

            // Act
            var expectedPackageId: String? = null
            setContent {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { action ->
                        val purchaseAction = action as PaywallAction.External.PurchasePackage
                        assertThat(purchaseAction.rcPackage).isNotNull
                        assertThat(purchaseAction.rcPackage?.identifier).isEqualTo(expectedPackageId)
                    },
                )
            }

            // Assert
            expectedPackageId = TestData.Packages.annual.identifier
            onNodeWithText(ctaPurchaseAnnual)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            expectedPackageId = TestData.Packages.monthly.identifier
            onNodeWithText(ctaPurchaseMonthly)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }

    @Test
    fun `If a purchase button is not inside a package component, the button should not be linked to any package`(): Unit =
        with(composeTestRule) {
            val ctaPurchase = "purchase"
            val selectAnnual = "select annual package"
            val selectMonthly = "select monthly package"
            val stackComponent = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = TestData.Packages.annual.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-annual"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PackageComponent(
                        packageId = TestData.Packages.monthly.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-monthly"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PurchaseButtonComponent(
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("purchase"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                ),
            )
            val packages = listOf(TestData.Packages.annual, TestData.Packages.monthly)
            val styleFactory = StyleFactory(
                localizations = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("purchase") to LocalizationData.Text(ctaPurchase),
                        LocalizationKey("select-annual") to LocalizationData.Text(selectAnnual),
                        LocalizationKey("select-monthly") to LocalizationData.Text(selectMonthly),
                    )
                ),
                offering = Offering(
                    identifier = "identifier",
                    serverDescription = "description",
                    metadata = emptyMap(),
                    availablePackages = packages,
                )
            )
            val style = styleFactory.create(stackComponent).getOrThrow().componentStyle as StackComponentStyle
            val state = FakePaywallState(packages = packages)

            // Act
            setContent {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { action ->
                        val purchaseAction = action as PaywallAction.External.PurchasePackage
                        assertThat(purchaseAction.rcPackage).isNull()
                    },
                )
            }

            // Assert
            onNodeWithText(selectAnnual)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            onNodeWithText(selectMonthly)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }

    @Test
    fun `If a web checkout purchase button is inside a package component, the button should be linked to that specific package`(): Unit =
        with(composeTestRule) {
            val ctaPurchaseAnnual = "purchase annual package"
            val ctaPurchaseMonthly = "purchase monthly package"
            val stackComponent = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = TestData.Packages.annual.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                PurchaseButtonComponent(
                                    stack = StackComponent(
                                        components = listOf(
                                            TextComponent(
                                                text = LocalizationKey("purchase-annual"),
                                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                            )
                                        )
                                    ),
                                    method = PurchaseButtonComponent.Method.WebCheckout(),
                                ),
                            )
                        )
                    ),
                    PackageComponent(
                        packageId = TestData.Packages.monthly.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                PurchaseButtonComponent(
                                    stack = StackComponent(
                                        components = listOf(
                                            TextComponent(
                                                text = LocalizationKey("purchase-monthly"),
                                                color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                            )
                                        )
                                    ),
                                    method = PurchaseButtonComponent.Method.WebCheckout(),
                                ),
                            )
                        )
                    ),
                ),
            )
            val packages = listOf(TestData.Packages.annual, TestData.Packages.monthly)
            val styleFactory = StyleFactory(
                localizations = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("purchase-annual") to LocalizationData.Text(ctaPurchaseAnnual),
                        LocalizationKey("purchase-monthly") to LocalizationData.Text(ctaPurchaseMonthly),
                    )
                ),
                offering = Offering(
                    identifier = "identifier",
                    serverDescription = "description",
                    metadata = emptyMap(),
                    availablePackages = packages,
                    webCheckoutURL = URL("https://test-wpl.revenuecat.com"),
                )
            )
            val style = styleFactory.create(stackComponent).getOrThrow().componentStyle as StackComponentStyle
            val state = FakePaywallState(packages = packages)

            // Act
            var expectedPackageId: String? = null
            setContent {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { clickAction ->
                        val action = clickAction as PaywallAction.External.LaunchWebCheckout
                        assertThat(action.customUrl).isNull()
                        assertThat(action.openMethod).isEqualTo(ButtonComponent.UrlMethod.EXTERNAL_BROWSER)
                        assertThat(action.autoDismiss).isTrue()
                        val packageParamBehavior = action.packageParamBehavior as PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append
                        assertThat(packageParamBehavior.rcPackage).isNotNull()
                        assertThat(packageParamBehavior.rcPackage?.identifier).isEqualTo(expectedPackageId)
                        assertThat(packageParamBehavior.packageParam).isNull()
                    },
                )
            }

            // Assert
            expectedPackageId = TestData.Packages.annual.identifier
            onNodeWithText(ctaPurchaseAnnual)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            expectedPackageId = TestData.Packages.monthly.identifier
            onNodeWithText(ctaPurchaseMonthly)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }


    @Test
    fun `If a web checkout purchase button is not inside a package component, the button should not be linked to any package`(): Unit =
        with(composeTestRule) {
            val ctaPurchase = "purchase"
            val selectAnnual = "select annual package"
            val selectMonthly = "select monthly package"
            val stackComponent = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = TestData.Packages.annual.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-annual"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PackageComponent(
                        packageId = TestData.Packages.monthly.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-monthly"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PurchaseButtonComponent(
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("purchase"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        ),
                        method = PurchaseButtonComponent.Method.WebCheckout(),
                    ),
                ),
            )
            val packages = listOf(TestData.Packages.annual, TestData.Packages.monthly)
            val styleFactory = StyleFactory(
                localizations = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("purchase") to LocalizationData.Text(ctaPurchase),
                        LocalizationKey("select-annual") to LocalizationData.Text(selectAnnual),
                        LocalizationKey("select-monthly") to LocalizationData.Text(selectMonthly),
                    )
                ),
                offering = Offering(
                    identifier = "identifier",
                    serverDescription = "description",
                    metadata = emptyMap(),
                    availablePackages = packages,
                )
            )
            val style = styleFactory.create(stackComponent).getOrThrow().componentStyle as StackComponentStyle
            val state = FakePaywallState(packages = packages)

            // Act
            setContent {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { clickAction ->
                        val action = clickAction as PaywallAction.External.LaunchWebCheckout
                        assertThat(action.customUrl).isNull()
                        assertThat(action.openMethod).isEqualTo(ButtonComponent.UrlMethod.EXTERNAL_BROWSER)
                        assertThat(action.autoDismiss).isTrue()
                        val packageParamBehavior = action.packageParamBehavior as PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append
                        assertThat(packageParamBehavior.rcPackage).isNull()
                        assertThat(packageParamBehavior.packageParam).isNull()
                    },
                )
            }

            // Assert
            onNodeWithText(selectAnnual)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            onNodeWithText(selectMonthly)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }


    @Test
    fun `Web product selection web checkout purchase button does not append Package`(): Unit =
        with(composeTestRule) {
            val ctaPurchase = "purchase"
            val selectAnnual = "select annual package"
            val selectMonthly = "select monthly package"
            val stackComponent = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = TestData.Packages.annual.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-annual"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PackageComponent(
                        packageId = TestData.Packages.monthly.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-monthly"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PurchaseButtonComponent(
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("purchase"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        ),
                        method = PurchaseButtonComponent.Method.WebProductSelection(),
                    ),
                ),
            )
            val packages = listOf(TestData.Packages.annual, TestData.Packages.monthly)
            val styleFactory = StyleFactory(
                localizations = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("purchase") to LocalizationData.Text(ctaPurchase),
                        LocalizationKey("select-annual") to LocalizationData.Text(selectAnnual),
                        LocalizationKey("select-monthly") to LocalizationData.Text(selectMonthly),
                        LocalizationKey("custom-checkout-url") to LocalizationData.Text("https://custom-checkout.revenuecat.com"),
                    )
                ),
                offering = Offering(
                    identifier = "identifier",
                    serverDescription = "description",
                    metadata = emptyMap(),
                    availablePackages = packages,
                )
            )
            val style = styleFactory.create(stackComponent).getOrThrow().componentStyle as StackComponentStyle
            val state = FakePaywallState(packages = packages)

            // Act
            setContent {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { clickAction ->
                        val action = clickAction as PaywallAction.External.LaunchWebCheckout
                        assertThat(action.customUrl).isNull()
                        assertThat(action.openMethod).isEqualTo(ButtonComponent.UrlMethod.EXTERNAL_BROWSER)
                        assertThat(action.autoDismiss).isTrue()
                        assertThat(action.packageParamBehavior).isInstanceOf(
                            PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.DoNotAppend::class.java
                        )
                    },
                )
            }

            // Assert
            onNodeWithText(selectAnnual)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            onNodeWithText(selectMonthly)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }

    @Test
    fun `Custom web checkout purchase button uses localized URL`(): Unit =
        with(composeTestRule) {
            val ctaPurchase = "purchase"
            val selectAnnual = "select annual package"
            val selectMonthly = "select monthly package"
            val stackComponent = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = TestData.Packages.annual.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-annual"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PackageComponent(
                        packageId = TestData.Packages.monthly.identifier,
                        isSelectedByDefault = false,
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("select-monthly"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        )
                    ),
                    PurchaseButtonComponent(
                        stack = StackComponent(
                            components = listOf(
                                TextComponent(
                                    text = LocalizationKey("purchase"),
                                    color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
                                )
                            )
                        ),
                        method = PurchaseButtonComponent.Method.CustomWebCheckout(
                            customUrl = PurchaseButtonComponent.CustomUrl(
                                urlLid = LocalizationKey("custom-checkout-url"),
                                packageParam = "my_custom_param",
                            ),
                        ),
                    ),
                ),
            )
            val packages = listOf(TestData.Packages.annual, TestData.Packages.monthly)
            val styleFactory = StyleFactory(
                localizations = nonEmptyMapOf(
                    LocaleId("en_US") to nonEmptyMapOf(
                        LocalizationKey("purchase") to LocalizationData.Text(ctaPurchase),
                        LocalizationKey("select-annual") to LocalizationData.Text(selectAnnual),
                        LocalizationKey("select-monthly") to LocalizationData.Text(selectMonthly),
                        LocalizationKey("custom-checkout-url") to LocalizationData.Text("https://custom-checkout.revenuecat.com"),
                    )
                ),
                offering = Offering(
                    identifier = "identifier",
                    serverDescription = "description",
                    metadata = emptyMap(),
                    availablePackages = packages,
                )
            )
            val style = styleFactory.create(stackComponent).getOrThrow().componentStyle as StackComponentStyle
            val state = FakePaywallState(packages = packages)

            // Act
            setContent {
                StackComponentView(
                    style = style,
                    state = state,
                    clickHandler = { clickAction ->
                        val action = clickAction as PaywallAction.External.LaunchWebCheckout
                        assertThat(action.customUrl).isEqualTo("https://custom-checkout.revenuecat.com")
                        assertThat(action.openMethod).isEqualTo(ButtonComponent.UrlMethod.EXTERNAL_BROWSER)
                        assertThat(action.autoDismiss).isTrue()
                        val packageParamBehavior = action.packageParamBehavior as PaywallAction.External.LaunchWebCheckout.PackageParamBehavior.Append
                        assertThat(packageParamBehavior.rcPackage).isNull()
                        assertThat(packageParamBehavior.packageParam).isEqualTo("my_custom_param")
                    },
                )
            }

            // Assert
            onNodeWithText(selectAnnual)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            onNodeWithText(selectMonthly)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
            onNodeWithText(ctaPurchase)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }

}
