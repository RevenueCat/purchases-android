package com.revenuecat.purchases.ui.revenuecatui.customercenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AppVersionComparisonTest {

    @Test
    fun `older patch is outdated`() {
        assertThat(isAppVersionOutdated("1.2.3", "1.2.4")).isTrue()
    }

    @Test
    fun `same version is not outdated`() {
        assertThat(isAppVersionOutdated("1.2.3", "1.2.3")).isFalse()
    }

    @Test
    fun `newer version is not outdated`() {
        assertThat(isAppVersionOutdated("1.3.0", "1.2.9")).isFalse()
    }

    @Test
    fun `compares numerically, not lexicographically`() {
        assertThat(isAppVersionOutdated("1.9.0", "1.10.0")).isTrue()
        assertThat(isAppVersionOutdated("1.10.0", "1.9.0")).isFalse()
    }

    @Test
    fun `missing components count as zero`() {
        assertThat(isAppVersionOutdated("2.1", "2.1.0")).isFalse()
        assertThat(isAppVersionOutdated("2.1", "2.1.1")).isTrue()
        assertThat(isAppVersionOutdated("2", "2.0.0")).isFalse()
    }

    @Test
    fun `prerelease and build suffixes are dropped`() {
        assertThat(isAppVersionOutdated("1.2.3-beta.1", "1.2.3")).isFalse()
        assertThat(isAppVersionOutdated("1.2.3+build42", "1.2.4")).isTrue()
    }

    @Test
    fun `unparseable versions never warn`() {
        assertThat(isAppVersionOutdated(null, "1.2.3")).isFalse()
        assertThat(isAppVersionOutdated("1.2.3", null)).isFalse()
        assertThat(isAppVersionOutdated("", "1.2.3")).isFalse()
        assertThat(isAppVersionOutdated("not-a-version", "1.2.3")).isFalse()
        assertThat(isAppVersionOutdated("1.2.3", "latest")).isFalse()
    }
}
