package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonComponentInteractionTests {

    // region componentInteraction

    @Test
    fun `CloseWorkflow componentInteraction emits close_workflow`() {
        assertThat(ButtonComponentStyle.Action.CloseWorkflow.componentInteraction(localeUrl = null))
            .isEqualTo(ButtonComponentInteraction(value = "close_workflow"))
    }

    @Test
    fun `NavigateBack componentInteraction emits navigate_back`() {
        assertThat(ButtonComponentStyle.Action.NavigateBack.componentInteraction(localeUrl = null))
            .isEqualTo(ButtonComponentInteraction(value = "navigate_back"))
    }

    @Test
    fun `RestorePurchases componentInteraction emits restore_purchases`() {
        assertThat(ButtonComponentStyle.Action.RestorePurchases.componentInteraction(localeUrl = null))
            .isEqualTo(ButtonComponentInteraction(value = "restore_purchases"))
    }

    @Test
    fun `WorkflowTrigger componentInteraction returns null`() {
        assertThat(ButtonComponentStyle.Action.WorkflowTrigger.componentInteraction(localeUrl = null))
            .isNull()
    }

    @Test
    fun `PurchasePackage componentInteraction returns null`() {
        assertThat(
            ButtonComponentStyle.Action.PurchasePackage(rcPackage = null)
                .componentInteraction(localeUrl = null)
        ).isNull()
    }

    @Test
    fun `NavigateTo CustomerCenter componentInteraction emits navigate_to_customer_center`() {
        val action = ButtonComponentStyle.Action.NavigateTo(
            ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter
        )
        assertThat(action.componentInteraction(localeUrl = null))
            .isEqualTo(ButtonComponentInteraction(value = "navigate_to_customer_center"))
    }

    @Test
    fun `NavigateTo Url componentInteraction emits navigate_to_url with the locale url`() {
        val action = ButtonComponentStyle.Action.NavigateTo(
            ButtonComponentStyle.Action.NavigateTo.Destination.Url(
                urls = nonEmptyMapOf(LocaleId("en_US") to "https://example.com"),
                method = ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
            )
        )
        assertThat(action.componentInteraction(localeUrl = "https://example.com"))
            .isEqualTo(ButtonComponentInteraction(value = "navigate_to_url", url = "https://example.com"))
    }

    // endregion

    // region isPurchaseRelated

    @Test
    fun `CloseWorkflow is not purchase related`() {
        assertThat(ButtonComponentStyle.Action.CloseWorkflow.isPurchaseRelated()).isFalse()
    }

    @Test
    fun `WorkflowTrigger is not purchase related`() {
        assertThat(ButtonComponentStyle.Action.WorkflowTrigger.isPurchaseRelated()).isFalse()
    }

    @Test
    fun `NavigateBack is not purchase related`() {
        assertThat(ButtonComponentStyle.Action.NavigateBack.isPurchaseRelated()).isFalse()
    }

    @Test
    fun `PurchasePackage is purchase related`() {
        assertThat(ButtonComponentStyle.Action.PurchasePackage(rcPackage = null).isPurchaseRelated())
            .isTrue()
    }

    @Test
    fun `WebCheckout is purchase related`() {
        assertThat(
            ButtonComponentStyle.Action.WebCheckout(
                rcPackage = null,
                autoDismiss = false,
                openMethod = ButtonComponent.UrlMethod.IN_APP_BROWSER,
            ).isPurchaseRelated()
        ).isTrue()
    }

    @Test
    fun `WebProductSelection is purchase related`() {
        assertThat(
            ButtonComponentStyle.Action.WebProductSelection(
                autoDismiss = false,
                openMethod = ButtonComponent.UrlMethod.IN_APP_BROWSER,
            ).isPurchaseRelated()
        ).isTrue()
    }

    // endregion
}
