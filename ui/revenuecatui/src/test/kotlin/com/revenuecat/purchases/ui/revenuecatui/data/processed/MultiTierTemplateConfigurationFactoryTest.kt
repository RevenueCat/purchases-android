package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template7
import com.revenuecat.purchases.ui.revenuecatui.errors.PackageConfigurationError
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
                TestData.Packages.semester,
                TestData.Packages.annual,
                TestData.Packages.lifetime,
            ),
            template = PaywallTemplate.TEMPLATE_7,
            storefrontCountryCode = "US",
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
            paywallData = TestData.template7,
            features = basicFeatures,
            tierId = "basic"
        )
        val bimonthlyPackage = TestData.Packages.bimonthly.getPackageInfoForTest(
            paywallData = TestData.template7,
            features = standardFeatures,
            tierId = "standard"
        )
        val quarterlyPackage = TestData.Packages.quarterly.getPackageInfoForTest(
            paywallData = TestData.template7,
            features = premiumFeatures,
            tierId = "premium"
        )
        val semesterPackage = TestData.Packages.semester.getPackageInfoForTest(
            paywallData = TestData.template7,
            features = standardFeatures,
            tierId = "standard"
        )
        val annualPackage = TestData.Packages.annual.getPackageInfoForTest(
            paywallData = TestData.template7,
            features = basicFeatures,
            tierId = "basic"
        )
        val lifetime = TestData.Packages.lifetime.getPackageInfoForTest(
            paywallData = TestData.template7,
            features = premiumFeatures,
            tierId = "premium"
        )

        val basicTier = TemplateConfiguration.TierInfo(
            name = "Basic",
            id = "basic",
            defaultPackage = annualPackage,
            // This order should match the basic tier in TestData.template7.
            packages = listOf(
                annualPackage,
                monthlyPackage,
            )
        )
        val standardTier = TemplateConfiguration.TierInfo(
            name = "Standard",
            id = "standard",
            defaultPackage = semesterPackage,
            // This order should match the standard tier in TestData.template7.
            packages = listOf(
                bimonthlyPackage,
                semesterPackage,
            )
        )
        val premiumTier = TemplateConfiguration.TierInfo(
            name = "Premium",
            id = "premium",
            defaultPackage = quarterlyPackage,
            // This order should match the premium tier in TestData.template7.
            packages = listOf(
                quarterlyPackage,
                lifetime,
            )
        )

        val expectedConfiguration = TemplateConfiguration.PackageConfiguration.MultiTier(
            defaultTier = basicTier,
            allTiers = listOf(basicTier, standardTier, premiumTier)
        )

        assertThat(packageConfiguration.defaultTier.packages.size).isEqualTo(2)
        packageConfiguration.defaultTier.packages.forEachIndexed { index, packageInfo ->
            assertThat(packageInfo)
                .describedAs("Failed to assert defaultTier package $index")
                .isEqualTo(expectedConfiguration.defaultTier.packages[index])
        }
        assertThat(packageConfiguration.defaultTier).isEqualTo(expectedConfiguration.defaultTier)
        assertThat(packageConfiguration.default).isEqualTo(expectedConfiguration.default)
        assertThat(packageConfiguration.all).containsExactly(*expectedConfiguration.all.toTypedArray())
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

    @Test
    fun `Should return failure if no tiers have any available products`() {
        val result = TemplateConfigurationFactory.create(
            variableDataProvider = variableDataProvider,
            mode = paywallMode,
            paywallData = TestData.template7.run {
                copy(
                    config = config.copy(
                        // Our config contains no packageIds.
                        packageIds = emptyList(),
                        // Our tiers contain 4 packages, but none of them are in availablePackages below.
                        tiers = listOf(
                            PaywallData.Configuration.Tier(
                                id = "basic",
                                packageIds = listOf(
                                    PackageType.ANNUAL.identifier!!,
                                    PackageType.MONTHLY.identifier!!,
                                ),
                                defaultPackageId = PackageType.ANNUAL.identifier!!,
                            ),
                            PaywallData.Configuration.Tier(
                                id = "standard",
                                packageIds = listOf(
                                    PackageType.TWO_MONTH.identifier!!,
                                    PackageType.SIX_MONTH.identifier!!,
                                ),
                                defaultPackageId = PackageType.SIX_MONTH.identifier!!,
                            ),
                        )
                    )
                )
            },
            // availablePackages contains some packages, but none of them are part of any tiers.
            availablePackages = listOf(
                TestData.Packages.weekly,
                TestData.Packages.lifetime,
            ),
            template = PaywallTemplate.TEMPLATE_7,
            storefrontCountryCode = "US",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(PackageConfigurationError::class.java)
        assertThat(result.exceptionOrNull()!!.message).isEqualTo("None of the tiers have any available products.")
    }

}
