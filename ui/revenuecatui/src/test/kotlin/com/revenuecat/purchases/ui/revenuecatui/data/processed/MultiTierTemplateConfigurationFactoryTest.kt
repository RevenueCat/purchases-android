package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template7
import com.revenuecat.purchases.ui.revenuecatui.helpers.getPackageInfoForTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class MultiTierTemplateConfigurationFactoryTest {

    private val paywallMode = PaywallMode.FULL_SCREEN

    private lateinit var variableDataProvider: VariableDataProvider

    private lateinit var template7Configuration: TemplateConfiguration

    @Before
    fun setUp() {
        variableDataProvider = VariableDataProvider(MockResourceProvider())
        val result = TemplateConfigurationFactory.create(
            variableDataProvider = variableDataProvider,
            mode = paywallMode,
            paywallData = TestData.template7,
            availablePackages = listOf(
                TestData.Packages.weekly,
                TestData.Packages.monthly,
                TestData.Packages.bimonthly,
                TestData.Packages.quarterly,
                TestData.Packages.annual,
                TestData.Packages.lifetime,
            ),
            activelySubscribedProductIdentifiers = setOf(
                TestData.Packages.monthly.product.id,
            ),
            nonSubscriptionProductIdentifiers = setOf(
                TestData.Packages.lifetime.product.id
            ),
            template = PaywallTemplate.TEMPLATE_7,
        )
        template7Configuration = result.getOrNull()!!
    }

    @Test
    fun `template configuration has correct template`() {
        assertThat(template7Configuration.template).isEqualTo(PaywallTemplate.TEMPLATE_7)
    }

    @Test
    fun `template configuration has correct view mode`() {
        assertThat(template7Configuration.mode).isEqualTo(paywallMode)
    }

    @Test
    fun `template configuration has correct paywall data configuration`() {
        assertThat(template7Configuration.configuration).isEqualTo(TestData.template7.config)
    }

    @Test
    fun `template configuration has correct package configuration`() {
        assertThat(template7Configuration.packages).isInstanceOf(
            TemplateConfiguration.PackageConfiguration.MultiTier::class.java
        )
        val packageConfiguration = template7Configuration.packages as TemplateConfiguration.PackageConfiguration.MultiTier

        val basicFeatures = listOf(
            PaywallData.LocalizedConfiguration.Feature(
                title = "Access to 10 cinematic LUTs",
                iconID = "tick",
            ),
            PaywallData.LocalizedConfiguration.Feature(
                title = "Standard fonts",
                iconID = "tick",
            ),
            PaywallData.LocalizedConfiguration.Feature(
                title = "2 templates",
                iconID = "tick",
            ),
        )
        val standardFeatures = listOf(
            PaywallData.LocalizedConfiguration.Feature(
                title = "Access to 30 cinematic LUTs",
                iconID = "tick",
            ),
            PaywallData.LocalizedConfiguration.Feature(
                title = "Pro fonts and transition effects",
                iconID = "tick",
            ),
            PaywallData.LocalizedConfiguration.Feature(
                title = "10+ templates",
                iconID = "tick",
            ),
        )
        val premiumFeatures = listOf(
            PaywallData.LocalizedConfiguration.Feature(
                title = "Access to all 150 of our cinematic LUTs",
                iconID = "tick",
            ),
            PaywallData.LocalizedConfiguration.Feature(
                title = "Custom design tools and transition effects",
                iconID = "tick",
            ),
            PaywallData.LocalizedConfiguration.Feature(
                title = "100+ exclusive templates",
                iconID = "tick",
            ),
        )

        val monthlyPackage = TestData.Packages.monthly.getPackageInfoForTest(
            currentlySubscribed = true,
            paywallData = TestData.template7,
            features = basicFeatures,
            tierName = "Basic"
        )
        val bimonthlyPackage = TestData.Packages.bimonthly.getPackageInfoForTest(
            currentlySubscribed = false,
            paywallData = TestData.template7,
            features = standardFeatures,
            tierName = "Standard"
        )
        val quarterlyPackage = TestData.Packages.quarterly.getPackageInfoForTest(
            currentlySubscribed = false,
            paywallData = TestData.template7,
            features = premiumFeatures,
            tierName = "Premium"
        )
        val semesterPackage = TestData.Packages.semester.getPackageInfoForTest(
            currentlySubscribed = false,
            paywallData = TestData.template7,
            features = standardFeatures,
            tierName = "Standard"
        )
        val annualPackage = TestData.Packages.annual.getPackageInfoForTest(
            currentlySubscribed = false,
            paywallData = TestData.template7,
            features = basicFeatures,
            tierName = "Basic"
        )
        val lifetime = TestData.Packages.lifetime.getPackageInfoForTest(
            currentlySubscribed = true,
            paywallData = TestData.template7,
            features = premiumFeatures,
            tierName = "Premium"
        )

        TemplateConfiguration.PackageConfiguration.MultiPackage(
            first = annualPackage,
            default = monthlyPackage,
            all = listOf(
                annualPackage,
                monthlyPackage,
            ),
        )

        val basicTier = TemplateConfiguration.TierInfo(
            name = "Basic",
            id = "basic",
            defaultPackage = annualPackage,
            packages = listOf(
                monthlyPackage,
                annualPackage,
            )
        )
        val standardTier = TemplateConfiguration.TierInfo(
            name = "Standard",
            id = "standard",
            defaultPackage = annualPackage,
            packages = listOf(
                bimonthlyPackage,
                semesterPackage,
            )
        )
        val premiumTier = TemplateConfiguration.TierInfo(
            name = "Premium",
            id = "premium",
            defaultPackage = annualPackage,
            packages = listOf(
                quarterlyPackage,
                lifetime,
            )
        )

        val expectedConfiguration = TemplateConfiguration.PackageConfiguration.MultiTier(
            firstTier = basicTier,
            allTiers = listOf(basicTier, standardTier, premiumTier)
        )

        assertThat(packageConfiguration.firstTier).isEqualTo(expectedConfiguration.firstTier)
//        assertThat(packageConfiguration.default).isEqualTo(expectedConfiguration.default)
//        assertThat(packageConfiguration.all).containsExactly(*expectedConfiguration.all.toTypedArray())
    }

    @Test
    fun `tier template configuration has correct images`() {
        assertThat(template7Configuration.imagesByTier["basic"]!!.headerUri)
            .isEqualTo(Uri.parse("https://assets.pawwalls.com/954459_1703109702.png"))
        assertThat(template7Configuration.imagesByTier["standard"]!!.headerUri)
            .isEqualTo(Uri.parse("https://assets.pawwalls.com/954459_1692992845.png"))
        assertThat(template7Configuration.imagesByTier["premium"]!!.headerUri)
            .isEqualTo(Uri.parse("https://assets.pawwalls.com/954459_1701267532.jpeg"))
    }

}
