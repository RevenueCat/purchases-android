package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import com.revenuecat.purchases.ui.revenuecatui.helpers.getPackageInfoForTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TemplateConfigurationFactoryTest {

    private val paywallMode = PaywallMode.FULL_SCREEN

    private lateinit var variableDataProvider: VariableDataProvider

    private lateinit var template2Configuration: TemplateConfiguration

    @Before
    fun setUp() {
        variableDataProvider = VariableDataProvider(MockResourceProvider())
        val result = TemplateConfigurationFactory.create(
            variableDataProvider = variableDataProvider,
            mode = paywallMode,
            paywallData = TestData.template2,
            availablePackages = listOf(
                TestData.Packages.weekly,
                TestData.Packages.monthly,
                TestData.Packages.annual,
                TestData.Packages.lifetime,
            ),
            activelySubscribedProductIdentifiers = setOf(
                TestData.Packages.monthly.product.id,
            ),
            nonSubscriptionProductIdentifiers = setOf(
                TestData.Packages.lifetime.product.id
            ),
            template = PaywallTemplate.TEMPLATE_2,
        )
        template2Configuration = result.getOrNull()!!
    }

    @Test
    fun `template configuration has correct template`() {
        assertThat(template2Configuration.template).isEqualTo(PaywallTemplate.TEMPLATE_2)
    }

    @Test
    fun `template configuration has correct view mode`() {
        assertThat(template2Configuration.mode).isEqualTo(paywallMode)
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

        val annualPackage = TestData.Packages.annual.getPackageInfoForTest(currentlySubscribed = false)
        val monthlyPackage = TestData.Packages.monthly.getPackageInfoForTest(currentlySubscribed = true)
        val lifetime = TestData.Packages.lifetime.getPackageInfoForTest(currentlySubscribed = true)

        val expectedConfiguration = TemplateConfiguration.PackageConfiguration.Multiple(
            first = annualPackage,
            default = monthlyPackage,
            all = listOf(
                annualPackage,
                monthlyPackage,
                lifetime
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

}
