@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.ProductChangeConfig
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
class WorkflowScreenMapperTest {

    private val defaultLocaleId = LocaleId("en_US")
    private val assetBaseURL = URL("https://assets.pawwalls.com")
    private val componentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            stack = StackComponent(components = emptyList()),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            stickyFooter = null,
        ),
    )
    private val localizations = mapOf(
        defaultLocaleId to mapOf(
            LocalizationKey("key") to LocalizationData.Text("value"),
        ),
    )

    private val stateDeclarations = mapOf(
        "selected_tab" to StateDeclaration(
            type = StateDeclaration.ValueType.STRING,
            defaultValue = JsonPrimitive("monthly"),
        ),
    )

    private val zeroDecimalPlaceCountries = listOf("TW", "MX")

    private val productChangeConfig = ProductChangeConfig(
        upgradeReplacementMode = StoreReplacementMode.CHARGE_FULL_PRICE,
        downgradeReplacementMode = StoreReplacementMode.DEFERRED,
    )

    private val screen = WorkflowScreen(
        name = "Test Screen",
        templateName = "template_v2",
        revision = 3,
        assetBaseURL = assetBaseURL,
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = "offering_id",
        zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        productChangeConfig = productChangeConfig,
        stateDeclarations = stateDeclarations,
    )

    @Test
    fun `toPaywallComponentsData maps all screen fields correctly`() {
        val screenId = "screen_abc"
        val data = WorkflowScreenMapper.toPaywallComponentsData(screen, screenId)

        assertThat(data.id).isEqualTo(screenId)
        assertThat(data.templateName).isEqualTo(screen.templateName)
        assertThat(data.assetBaseURL).isEqualTo(screen.assetBaseURL)
        assertThat(data.componentsConfig).isEqualTo(screen.componentsConfig)
        assertThat(data.componentsLocalizations).isEqualTo(screen.componentsLocalizations)
        assertThat(data.defaultLocaleIdentifier).isEqualTo(screen.defaultLocaleIdentifier)
        assertThat(data.revision).isEqualTo(screen.revision)
        assertThat(data.zeroDecimalPlaceCountries).isEqualTo(zeroDecimalPlaceCountries)
        assertThat(data.productChangeConfig).isEqualTo(productChangeConfig)
        assertThat(data.stateDeclarations).isEqualTo(stateDeclarations)
    }

    @Test
    fun `toPaywallComponentsData maps automaticallyScaleFontSize`() {
        val data = WorkflowScreenMapper.toPaywallComponentsData(
            screen = screen.copy(automaticallyScaleFontSize = false),
            screenId = "screen_abc",
        )

        assertThat(data.automaticallyScaleFontSize).isFalse()
    }

    @Test
    fun `toPaywallComponents uses provided uiConfig`() {
        val screenId = "screen_abc"
        val uiConfig = UiConfig()
        val paywallComponents = WorkflowScreenMapper.toPaywallComponents(screen, screenId, uiConfig)

        assertThat(paywallComponents.uiConfig).isEqualTo(uiConfig)
        assertThat(paywallComponents.data.getOrThrow()).isEqualTo(WorkflowScreenMapper.toPaywallComponentsData(screen, screenId))
    }
}
