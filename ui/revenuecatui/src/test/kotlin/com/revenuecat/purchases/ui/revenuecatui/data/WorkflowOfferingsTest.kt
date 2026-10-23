package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WorkflowOfferingsTest {

    private val fetchedOffering = TestData.template2Offering
    private val otherOffering = TestData.template1Offering
    private val offerings = Offerings(
        current = fetchedOffering,
        all = mapOf(
            fetchedOffering.identifier to fetchedOffering,
            otherOffering.identifier to otherOffering,
        ),
    )
    private val developerProvidedOffering = Offering(
        identifier = fetchedOffering.identifier,
        serverDescription = "modified",
        metadata = mapOf("custom_key" to "custom_value"),
        availablePackages = listOf(TestData.Packages.monthly),
    )

    @Test
    fun `returns the developer-provided offering when the identifier matches`() {
        val workflowOfferings = WorkflowOfferings(offerings, developerProvidedOffering)

        assertThat(workflowOfferings[fetchedOffering.identifier]).isSameAs(developerProvidedOffering)
    }

    @Test
    fun `returns the fetched offering when the identifier does not match the developer-provided one`() {
        val workflowOfferings = WorkflowOfferings(offerings, developerProvidedOffering)

        assertThat(workflowOfferings[otherOffering.identifier]).isSameAs(otherOffering)
    }

    @Test
    fun `returns the fetched offering when there is no developer-provided offering`() {
        val workflowOfferings = WorkflowOfferings(offerings, developerProvidedOffering = null)

        assertThat(workflowOfferings[fetchedOffering.identifier]).isSameAs(fetchedOffering)
    }

    @Test
    fun `returns null when the identifier is in neither`() {
        val workflowOfferings = WorkflowOfferings(offerings, developerProvidedOffering)

        assertThat(workflowOfferings["missing"]).isNull()
    }
}
