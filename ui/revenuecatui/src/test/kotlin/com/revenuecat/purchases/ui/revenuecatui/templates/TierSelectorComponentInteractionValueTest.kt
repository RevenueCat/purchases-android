package com.revenuecat.purchases.ui.revenuecatui.templates

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.getPackageInfoForTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TierSelectorComponentInteractionValueTest {

    private fun tier(name: String, id: String): TemplateConfiguration.TierInfo {
        val rcPackage = TestData.template2Offering.availablePackages.first { it.packageType == PackageType.MONTHLY }
        val pkg = rcPackage.getPackageInfoForTest()
        return TemplateConfiguration.TierInfo(
            name = name,
            id = id,
            defaultPackage = pkg,
            packages = listOf(pkg),
        )
    }

    @Test
    fun `uses non-blank name`() {
        assertThat(tierSelectorComponentInteractionValue(tier("Premium", "premium_id"))).isEqualTo("Premium")
    }

    @Test
    fun `blank name matches empty string fallback`() {
        assertThat(tierSelectorComponentInteractionValue(tier("", "tier_abc"))).isEmpty()
    }

    @Test
    fun `whitespace-only name matches empty string fallback`() {
        assertThat(tierSelectorComponentInteractionValue(tier("   ", "id_only"))).isEmpty()
    }
}
