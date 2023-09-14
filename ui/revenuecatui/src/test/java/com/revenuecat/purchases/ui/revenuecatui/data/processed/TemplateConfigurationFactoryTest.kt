package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
internal class TemplateConfigurationFactoryTest {

    private val paywallViewMode = PaywallViewMode.FULL_SCREEN

    private val template2Configuration = TemplateConfigurationFactory.create(
        paywallViewMode,
        TestData.template2,
        listOf(TestData.Packages.weekly, TestData.Packages.monthly, TestData.Packages.annual),
        emptySet(),
        Locale.US,
    )

    @Test
    fun `template configuration has correct template`() {
        assertThat(template2Configuration.template).isEqualTo(PaywallTemplate.TEMPLATE_2)
    }

    @Test
    fun `template configuration has correct view mode`() {
        assertThat(template2Configuration.mode).isEqualTo(paywallViewMode)
    }

    @Test
    fun `template configuration has correct paywall data configuration`() {
        assertThat(template2Configuration.configuration).isEqualTo(TestData.template2.config)
    }

    @Test
    fun `template configuration has correct package configuration`() {
        assertThat(template2Configuration.packages).isInstanceOf(
            TemplateConfiguration.PackageConfiguration.Multiple::class.java
        )
        val packageConfiguration = template2Configuration.packages as TemplateConfiguration.PackageConfiguration.Multiple

        val expectedConfiguration = TemplateConfiguration.PackageConfiguration.Multiple(
            first = getPackageInfo(TestData.Packages.weekly),
            default = getPackageInfo(TestData.Packages.monthly),
            all = listOf(
                getPackageInfo(TestData.Packages.weekly),
                getPackageInfo(TestData.Packages.monthly),
                getPackageInfo(TestData.Packages.annual)
            ),
        )
        assertThat(packageConfiguration).isEqualTo(expectedConfiguration)
    }

    @Test
    fun `template configuration has correct images`() {
        assertThat(template2Configuration.images.iconUri)
            .isEqualTo(Uri.parse("https://assets.pawwalls.com/9a17e0a7_1689854430..jpeg"))
        assertThat(template2Configuration.images.backgroundUri)
            .isEqualTo(Uri.parse("https://assets.pawwalls.com/9a17e0a7_1689854342..jpg"))
        assertThat(template2Configuration.images.headerUri)
            .isEqualTo(Uri.parse("https://assets.pawwalls.com/9a17e0a7_1689854430..jpeg"))
    }

    private fun getPackageInfo(rcPackage: Package): TemplateConfiguration.PackageInfo {
        val processedLocalization = ProcessedLocalizedConfiguration(
            title = "Call to action for better conversion.",
            subtitle = "Lorem ipsum is simply dummy text of the printing and typesetting industry.",
            callToAction = "Subscribe for {{ price_per_period }}",
            callToActionWithIntroOffer = null,
            offerDetails = "{{ total_price_and_per_month }}",
            offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
            offerName = "{{ sub_period }}",
            features = emptyList(),
        )
        return TemplateConfiguration.PackageInfo(
            rcPackage = rcPackage,
            localization = processedLocalization,
            currentlySubscribed = false,
            discountRelativeToMostExpensivePerMonth = null,
        )
    }
}
