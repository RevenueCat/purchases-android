package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.Locale.getDefault

@RunWith(AndroidJUnit4::class)
class PathUtilsTest {

    @Test
    fun `filterSubscriptionSpecificPaths includes CANCEL paths`() {
        val paths = listOf(
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL),
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("cancel")
    }

    @Test
    fun `filterSubscriptionSpecificPaths includes REFUND_REQUEST paths`() {
        val paths = listOf(
            createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST),
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("refund")
    }

    @Test
    fun `filterSubscriptionSpecificPaths includes CHANGE_PLANS paths`() {
        val paths = listOf(
            createHelpPath("change", CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS),
            createHelpPath("unknown", CustomerCenterConfigData.HelpPath.PathType.UNKNOWN),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("change")
    }

    @Test
    fun `filterSubscriptionSpecificPaths includes CUSTOM_ACTION paths`() {
        val paths = listOf(
            createHelpPath("custom_action", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION),
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("custom_action")
    }

    @Test
    fun `filterSubscriptionSpecificPaths excludes MISSING_PURCHASE paths`() {
        val paths = listOf(
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL),
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("cancel")
    }

    @Test
    fun `filterSubscriptionSpecificPaths includes CUSTOM_URL paths`() {
        val paths = listOf(
            createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL),
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("custom_url")
    }

    @Test
    fun `filterSubscriptionSpecificPaths excludes UNKNOWN paths`() {
        val paths = listOf(
            createHelpPath("change", CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS),
            createHelpPath("unknown", CustomerCenterConfigData.HelpPath.PathType.UNKNOWN),
        )

        val result = PathUtils.filterSubscriptionSpecificPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("change")
    }

    @Test
    fun `filterGeneralPaths includes MISSING_PURCHASE paths`() {
        val paths = listOf(
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("missing")
    }

    @Test
    fun `filterGeneralPaths includes CUSTOM_URL paths`() {
        val paths = listOf(
            createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL),
            createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("custom_url")
    }

    @Test
    fun `filterGeneralPaths includes UNKNOWN paths`() {
        val paths = listOf(
            createHelpPath("unknown", CustomerCenterConfigData.HelpPath.PathType.UNKNOWN),
            createHelpPath("change", CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("unknown")
    }

    @Test
    fun `filterGeneralPaths includes CUSTOM_ACTION paths`() {
        val paths = listOf(
            createHelpPath("custom_action", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION),
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL),
            createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("custom_action")
    }

    @Test
    fun `filterGeneralPaths excludes CANCEL paths`() {
        val paths = listOf(
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE),
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("missing")
    }

    @Test
    fun `filterGeneralPaths excludes REFUND_REQUEST paths`() {
        val paths = listOf(
            createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL),
            createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("custom_url")
    }

    @Test
    fun `filterGeneralPaths excludes CHANGE_PLANS paths`() {
        val paths = listOf(
            createHelpPath("unknown", CustomerCenterConfigData.HelpPath.PathType.UNKNOWN),
            createHelpPath("change", CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS),
        )

        val result = PathUtils.filterGeneralPaths(paths)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("unknown")
    }

    @Test
    fun `getButtonStyleForPath returns FILLED for CANCEL paths`() {
        val path = createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.FILLED)
    }

    @Test
    fun `getButtonStyleForPath returns FILLED for REFUND_REQUEST paths`() {
        val path = createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.FILLED)
    }

    @Test
    fun `getButtonStyleForPath returns FILLED for CHANGE_PLANS paths`() {
        val path = createHelpPath("change", CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.FILLED)
    }

    @Test
    fun `getButtonStyleForPath returns FILLED for CUSTOM_ACTION paths`() {
        val path = createHelpPath("custom_action", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.FILLED)
    }

    @Test
    fun `getButtonStyleForPath returns OUTLINED for MISSING_PURCHASE paths`() {
        val path = createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.OUTLINED)
    }

    @Test
    fun `getButtonStyleForPath returns FILLED for CUSTOM_URL paths`() {
        val path = createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.FILLED)
    }

    @Test
    fun `getButtonStyleForPath returns OUTLINED for UNKNOWN paths`() {
        val path = createHelpPath("unknown", CustomerCenterConfigData.HelpPath.PathType.UNKNOWN)

        val result = PathUtils.getButtonStyleForPath(path)

        assertThat(result).isEqualTo(SettingsButtonStyle.OUTLINED)
    }

    @Test
    fun `sortPathsByButtonPriority puts FILLED buttons before OUTLINED buttons`() {
        val paths = listOf(
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE), // OUTLINED
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL), // FILLED
            createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL), // FILLED
            createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST), // FILLED
        )

        val result = PathUtils.sortPathsByButtonPriority(paths)

        assertThat(result).hasSize(4)
        assertThat(result.take(3).map { it.id }).containsExactlyInAnyOrder("cancel", "refund", "custom_url")
        assertThat(result[3].id).isEqualTo("missing")
    }

    @Test
    fun `sortPathsByButtonPriority puts subscription-specific FILLED buttons before general OUTLINED buttons`() {
        val paths = listOf(
            createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL), // FILLED
            createHelpPath("custom_action", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION), // FILLED
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL), // FILLED
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE), // OUTLINED
        )

        val result = PathUtils.sortPathsByButtonPriority(paths)

        assertThat(result).hasSize(4)
        assertThat(result.take(3).map { it.id }).containsExactlyInAnyOrder("cancel", "custom_action", "custom_url")
        assertThat(result[3].id).isEqualTo("missing")
    }

    @Test
    fun `sortPathsByButtonPriority groups all subscription-specific paths together`() {
        val paths = listOf(
            createHelpPath("unknown", CustomerCenterConfigData.HelpPath.PathType.UNKNOWN), // OUTLINED
            createHelpPath("custom_action", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_ACTION), // FILLED
            createHelpPath("missing", CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE), // OUTLINED
            createHelpPath("cancel", CustomerCenterConfigData.HelpPath.PathType.CANCEL), // FILLED
            createHelpPath("custom_url", CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL), // FILLED
            createHelpPath("change", CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS), // FILLED
            createHelpPath("refund", CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST), // FILLED
        )

        val result = PathUtils.sortPathsByButtonPriority(paths)

        assertThat(result).hasSize(7)
        // First 5 should be FILLED (subscription-specific)
        assertThat(result.take(5).map { it.id }).containsExactlyInAnyOrder(
            "cancel",
            "change",
            "refund",
            "custom_action",
            "custom_url",
        )
        // Last 2 should be OUTLINED (general)
        assertThat(result.takeLast(2).map { it.id }).containsExactlyInAnyOrder(
            "unknown",
            "missing",
        )
    }

    private fun createHelpPath(
        id: String,
        type: CustomerCenterConfigData.HelpPath.PathType,
    ): CustomerCenterConfigData.HelpPath {
        id.replace("_", " ")
        return CustomerCenterConfigData.HelpPath(
            id = id,
            title = id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() },
            type = type,
        )
    }
}
