package com.revenuecat.purchases.ui.revenuecatui.customercenter

import com.revenuecat.purchases.customercenter.CustomActionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomActionDataTest {

    @Test
    fun `CustomActionData creates correctly with both parameters`() {
        val actionIdentifier = "delete_user"
        val purchaseIdentifier = "monthly_subscription"
        
        val customActionData = CustomActionData(
            actionIdentifier = actionIdentifier,
            purchaseIdentifier = purchaseIdentifier
        )
        
        assertThat(customActionData.actionIdentifier).isEqualTo(actionIdentifier)
        assertThat(customActionData.purchaseIdentifier).isEqualTo(purchaseIdentifier)
    }

    @Test
    fun `CustomActionData creates correctly with null purchase identifier`() {
        val actionIdentifier = "rate_app"
        
        val customActionData = CustomActionData(
            actionIdentifier = actionIdentifier,
            purchaseIdentifier = null
        )
        
        assertThat(customActionData.actionIdentifier).isEqualTo(actionIdentifier)
        assertThat(customActionData.purchaseIdentifier).isNull()
    }

    @Test
    fun `CustomActionData equality works correctly`() {
        val customActionData1 = CustomActionData("action1", "purchase1")
        val customActionData2 = CustomActionData("action1", "purchase1")
        val customActionData3 = CustomActionData("action2", "purchase1")
        
        assertThat(customActionData1).isEqualTo(customActionData2)
        assertThat(customActionData1).isNotEqualTo(customActionData3)
    }
} 