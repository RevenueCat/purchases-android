@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
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

    private val screen = WorkflowScreen(
        name = "Test Screen",
        templateName = "template_v2",
        revision = 3,
        assetBaseURL = assetBaseURL,
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringId = "offering_id",
    )

    @Test
    fun `toPaywallComponentsData maps all screen fields correctly`() {
        val data = WorkflowScreenMapper.toPaywallComponentsData(screen)

        assertThat(data.templateName).isEqualTo(screen.templateName)
        assertThat(data.assetBaseURL).isEqualTo(screen.assetBaseURL)
        assertThat(data.componentsConfig).isEqualTo(screen.componentsConfig)
        assertThat(data.componentsLocalizations).isEqualTo(screen.componentsLocalizations)
        assertThat(data.defaultLocaleIdentifier).isEqualTo(screen.defaultLocaleIdentifier)
        assertThat(data.revision).isEqualTo(screen.revision)
    }

    @Test
    fun `toPaywallComponents uses provided uiConfig`() {
        val uiConfig = UiConfig()
        val paywallComponents = WorkflowScreenMapper.toPaywallComponents(screen, uiConfig)

        assertThat(paywallComponents.uiConfig).isEqualTo(uiConfig)
        assertThat(paywallComponents.data).isEqualTo(WorkflowScreenMapper.toPaywallComponentsData(screen))
    }
}
