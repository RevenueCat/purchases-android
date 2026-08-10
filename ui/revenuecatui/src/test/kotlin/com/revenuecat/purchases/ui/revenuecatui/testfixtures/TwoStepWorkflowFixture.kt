package com.revenuecat.purchases.ui.revenuecatui.testfixtures

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.Offering
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
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

/**
 * The minimal two-step workflow shared by the ViewModel and composition tests: `step-1` navigates to
 * `step-2` when `btn-next` is pressed, both steps render the same single-package screen, and both share
 * one offering.
 *
 * Kept in one place so a constructor change to [WorkflowScreen], [WorkflowStep] or [PublishedWorkflow]
 * is a single edit, and so the two test classes cannot drift into disagreeing about "the same" workflow.
 */
internal object TwoStepWorkflowFixture {

    const val OFFERING_ID = "test_offering"
    const val INITIAL_STEP_ID = "step-1"
    const val SECOND_STEP_ID = "step-2"
    const val NEXT_BUTTON_ID = "btn-next"

    private const val SCREEN_ID_1 = "screen-1"
    private const val SCREEN_ID_2 = "screen-2"

    val defaultLocaleId = LocaleId("en_US")

    val localizations = mapOf(
        defaultLocaleId to mapOf(
            LocalizationKey("dummy_text") to LocalizationData.Text("dummy"),
        ),
    )

    val componentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            // At least one PackageComponent is required for calculateState to produce
            // PaywallState.Loaded.Components instead of PaywallState.Error.
            stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            stickyFooter = null,
        ),
    )

    fun makeScreen(screenId: String) = WorkflowScreen(
        name = screenId,
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = OFFERING_ID,
    )

    val step1 = WorkflowStep(
        id = INITIAL_STEP_ID,
        type = "screen",
        screenId = SCREEN_ID_1,
        triggers = listOf(
            WorkflowTrigger(
                name = "Next",
                type = WorkflowTriggerType.ON_PRESS,
                actionId = "action-next",
                componentId = NEXT_BUTTON_ID,
            ),
        ),
        triggerActions = mapOf("action-next" to WorkflowTriggerAction.Step(stepId = SECOND_STEP_ID)),
    )

    val step2 = WorkflowStep(
        id = SECOND_STEP_ID,
        type = "screen",
        screenId = SCREEN_ID_2,
        triggers = emptyList(),
        triggerActions = emptyMap(),
    )

    val workflow = PublishedWorkflow(
        id = "wfl-test",
        displayName = "Test",
        initialStepId = INITIAL_STEP_ID,
        steps = mapOf(INITIAL_STEP_ID to step1, SECOND_STEP_ID to step2),
        screens = mapOf(SCREEN_ID_1 to makeScreen(SCREEN_ID_1), SCREEN_ID_2 to makeScreen(SCREEN_ID_2)),
        metadata = emptyMap(),
        singleStepFallbackId = INITIAL_STEP_ID,
    )

    val offering = Offering(
        identifier = OFFERING_ID,
        serverDescription = "",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = null,
        webCheckoutURL = null,
    )

    val offerings = Offerings(offering, mapOf(OFFERING_ID to offering))
}
