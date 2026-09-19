package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeriodTypeLabelTest {

    private val localization = CustomerCenterConfigData.Localization(
        locale = "en_US",
        localizedStrings = emptyMap(),
    )

    @Test
    fun `trial shows the trial period label`() {
        assertThat(periodTypeLabel(PeriodType.TRIAL, localization)).isEqualTo("Trial Period")
    }

    @Test
    fun `intro shows the introductory price label`() {
        assertThat(periodTypeLabel(PeriodType.INTRO, localization)).isEqualTo("Introductory Price")
    }

    @Test
    fun `normal shows no period row`() {
        assertThat(periodTypeLabel(PeriodType.NORMAL, localization)).isNull()
    }

    @Test
    fun `prepaid shows no period row rather than mislabeling it as introductory`() {
        assertThat(periodTypeLabel(PeriodType.PREPAID, localization)).isNull()
    }
}
