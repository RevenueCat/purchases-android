package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
internal class TemplateConfigurationFactoryTest {

    private val paywallViewMode = PaywallViewMode.FULL_SCREEN

    private lateinit var variableDataProvider: VariableDataProvider

    private lateinit var template2Configuration: TemplateConfiguration

    @Before
    fun setUp() {
        variableDataProvider = VariableDataProvider(MockApplicationContext())
        val result = TemplateConfigurationFactory.create(
            variableDataProvider,
            paywallViewMode,
            TestData.template2,
            listOf(TestData.Packages.weekly, TestData.Packages.monthly, TestData.Packages.annual),
            emptySet(),
            PaywallTemplate.TEMPLATE_2,
        )
        template2Configuration = result.getOrNull()!!
    }

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
            first = getPackageInfo(TestData.Packages.annual),
            default = getPackageInfo(TestData.Packages.monthly),
            all = listOf(
                getPackageInfo(TestData.Packages.annual),
                getPackageInfo(TestData.Packages.monthly),
            ),
        )

        assertThat(packageConfiguration.first).isEqualTo(expectedConfiguration.first)
        assertThat(packageConfiguration.default).isEqualTo(expectedConfiguration.default)
        assertThat(packageConfiguration.all).containsExactly(*expectedConfiguration.all.toTypedArray())
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
        val localizedConfiguration = TestData.template2.configForLocale(Locale.US)!!
        val periodName = when(rcPackage.packageType) {
            PackageType.ANNUAL -> "Annual"
            PackageType.MONTHLY -> "Monthly"
            PackageType.WEEKLY -> "Weekly"
            else -> error("Unknown package type ${rcPackage.packageType}")
        }
        val callToAction = when(rcPackage.packageType) {
            PackageType.ANNUAL -> "Subscribe for $67.99/yr"
            PackageType.MONTHLY -> "Subscribe for $7.99/mth"
            PackageType.WEEKLY -> "Subscribe for $1.99/wk"
            else -> error("Unknown package type ${rcPackage.packageType}")
        }
        val offerDetails = when(rcPackage.packageType) {
            PackageType.ANNUAL -> "$67.99/yr ($5.67/mth)"
            PackageType.MONTHLY -> "$7.99/mth"
            PackageType.WEEKLY -> "$1.99/wk ($7.96/mth)"
            else -> error("Unknown package type ${rcPackage.packageType}")
        }
        val offerDetailsWithIntroOffer = when(rcPackage.packageType) {
            PackageType.ANNUAL -> "$67.99/yr ($5.67/mth) after 1 month trial"
            PackageType.MONTHLY -> "$7.99/mth after  trial"
            PackageType.WEEKLY -> "$1.99/wk ($7.96/mth) after  trial"
            else -> error("Unknown package type ${rcPackage.packageType}")
        }
        val processedLocalization = ProcessedLocalizedConfiguration(
            title = localizedConfiguration.title,
            subtitle = localizedConfiguration.subtitle,
            callToAction = callToAction,
            callToActionWithIntroOffer = null,
            callToActionWithMultipleIntroOffers = null,
            offerDetails = offerDetails,
            offerDetailsWithIntroOffer = offerDetailsWithIntroOffer,
            offerDetailsWithMultipleIntroOffers = null,
            offerName = periodName,
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
