package com.revenuecat.purchases.customercenter.events

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID

/**
 * Type representing a customer center event and associated data. Meant for RevenueCatUI use.
 */
@InternalRevenueCatAPI
@Poko
public class CustomerCenterSurveyOptionChosenEvent(
    public val creationData: CreationData = CreationData(),
    public val data: Data,
) : FeatureEvent {

    public companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal val json = Json.Default
    }

    @InternalRevenueCatAPI
    @Poko
    public class CreationData(
        val id: UUID = UUID.randomUUID(),
        val date: Date = Date(),
    )

    @InternalRevenueCatAPI
    @Poko
    @SuppressWarnings("LongParameterList")
    public class Data(
        val timestamp: Date,
        val darkMode: Boolean,
        val locale: String,
        val version: Int = 1,
        val revisionID: Int = 1,
        val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        val path: CustomerCenterConfigData.HelpPath.PathType,
        val url: String?, // URL if CUSTOM_URL
        val surveyOptionID: String,
        val additionalContext: String? = null, // null for now until we support

        // isSandbox not available in Android
    ) {
        val type: CustomerCenterEventType = CustomerCenterEventType.SURVEY_OPTION_CHOSEN
    }
}
