// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.robolectric.shadows.ShadowLooper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class CustomerCenterPreviewWorkflowTest {
    @Test
    fun `injected preview traverses steps and restores without singleton or SDK workflow fetching`() = runBlocking {
        val store = ViewModelStore()
        try {
            val offering = TestData.template1Offering
            val offerings = Offerings(offering, mapOf(offering.identifier to offering))
            val components = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
                    stickyFooter = null,
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                ),
            )
            fun screen() = WorkflowScreen(
                templateName = "template_v2",
                revision = 1,
                assetBaseURL = URL("https://assets.paywalls.com"),
                componentsConfig = components,
                componentsLocalizations = mapOf(
                    LocaleId("en_US") to mapOf(LocalizationKey("dummy") to LocalizationData.Text("dummy")),
                ),
                defaultLocaleIdentifier = LocaleId("en_US"),
                offeringIdentifier = offering.identifier,
            )
            val parameters = mapOf(
                "offering" to JsonObject(mapOf("identifier" to JsonPrimitive(offering.identifier))),
            )
            val workflow = PublishedWorkflow(
                "preview-workflow", "Preview workflow", "first",
                steps = mapOf(
                    "first" to WorkflowStep(
                        id = "first", type = "screen", screenId = "first", paramValues = parameters,
                        triggers = listOf(
                            WorkflowTrigger("next", WorkflowTriggerType.ON_PRESS, "next", "next-button"),
                        ),
                        triggerActions = mapOf("next" to WorkflowTriggerAction.Step("second")),
                    ),
                    "second" to WorkflowStep(
                        id = "second", type = "screen", screenId = "second", paramValues = parameters,
                    ),
                ),
                screens = mapOf("first" to screen(), "second" to screen()),
            )
            val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
            coEvery { provider.customerInfo() } returns mockk<CustomerInfo>(relaxed = true)
            coEvery { provider.restorePurchases() } returns mockk<CustomerInfo>(relaxed = true)
            val injected = CustomerCenterPreviewWorkflow(workflow, offerings, UiConfig())
            val options = previewPaywallOptions(offering, injected) {}
            assertThat(options.injectedWorkflowOfferings).isSameAs(offerings)
            val model = PaywallViewModelImpl(
                MockResourceProvider(), CustomerCenterPreviewPurchases(provider),
                options, TestData.Constants.currentColorScheme, false, null,
                backgroundDispatcher = Dispatchers.Unconfined,
            )
            store.put("preview", model)
            ShadowLooper.idleMainLooper()
            assertThat(model.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
            assertThat(model.workflowState.value?.currentStepId).isEqualTo("first")
            model.handleWorkflowAction("next-button", WorkflowTriggerType.ON_PRESS)
            ShadowLooper.idleMainLooper()
            assertThat(model.workflowState.value?.currentStepId).isEqualTo("second")
            model.handleRestorePurchases()
            ShadowLooper.idleMainLooper()
            coVerify(exactly = 1) { provider.restorePurchases() }
        } finally {
            store.clear()
        }
    }
}
