package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStickyFooterComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class FooterPackageSelectionTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun state(hidden: Boolean) = FakePaywallState(
        components = listOf(PackageComponent(
            packageId = TestData.Packages.monthly.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(emptyList()),
        )),
        packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        addPackageComponents = false,
        stateDeclarations = mapOf("hide_footer" to StateDeclaration(
            type = StateDeclaration.ValueType.BOOLEAN, defaultValue = JsonPrimitive(hidden),
        )),
        stickyFooter = StickyFooterComponent(
            stack = StackComponent(listOf(PackageComponent(
                packageId = TestData.Packages.annual.identifier,
                isSelectedByDefault = true,
                stack = StackComponent(emptyList(), size = Size(
                    width = SizeConstraint.Fill(), height = SizeConstraint.Fixed(60u),
                )),
            ))),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.State(
                    operator = ComponentOverride.EqualityOperator.EQUALS,
                    name = "hide_footer", value = JsonPrimitive(true),
                )),
                properties = PartialStickyFooterComponent(layout = StickyFooterComponent.Layout.HIDDEN),
            )),
        ),
    )

    @Test
    fun `initial state hidden footer selects visible monthly package`() {
        assertThat(state(hidden = true).selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `hiding footer after first render reconciles selection`() {
        val state = state(hidden = false)
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.fillMaxSize()) {
                OverlayLayout(state, Modifier.fillMaxSize(), hasFooter = true) {
                    Box(Modifier.fillMaxSize())
                    ComponentView(
                        style = checkNotNull(state.stickyFooter), state = state, onClick = {},
                        modifier = Modifier.testTag("footer"),
                    )
                }
            }
        }
        composeTestRule.runOnIdle {
            assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
            state.stateStore.applyUpdates(listOf(
                StateUpdate.Set("hide_footer", StateUpdateValue.Literal(JsonPrimitive(true))),
            ))
        }
        composeTestRule.onNodeWithTag("footer").assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
            state.stateStore.applyUpdates(listOf(
                StateUpdate.Set("hide_footer", StateUpdateValue.Literal(JsonPrimitive(false))),
            ))
        }
        composeTestRule.onNodeWithTag("footer").assertExists()
        composeTestRule.runOnIdle {
            assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
        }
    }
}
