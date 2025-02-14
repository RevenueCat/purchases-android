package com.revenuecat.purchases.customercenter.events

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID

/**
 * Type representing a customer center event and associated data. Meant for RevenueCatUI use.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Poko
class CustomerCenterSurveyOptionChosenEvent(
    val creationData: CreationData = CreationData(),
    val data: Data,
) : FeatureEvent {

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val json = Json.Default
    }

    @ExperimentalPreviewRevenueCatPurchasesAPI
    @Poko
    class CreationData(
        val id: UUID = UUID.randomUUID(),
        val date: Date = Date(),
    )

    @ExperimentalPreviewRevenueCatPurchasesAPI
    @Poko
    @SuppressWarnings("LongParameterList")
    class Data(
        val timestamp: Date,
        val darkMode: Boolean,
        val locale: String,
        val version: Int = 1,
        val revisionID: Int = 1,
        val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        val path: CustomerCenterConfigData.HelpPath.PathType,
        val url: String?, // URL if CUSTOM_URL
        val surveyOptionID: String,
        val surveyOptionTitleKey: String,
        val additionalContext: String? = null, // null for now until we support

        // isSandbox not available in Android
    ) {
        val type: CustomerCenterEventType = CustomerCenterEventType.SURVEY_OPTION_CHOSEN
    }
}
