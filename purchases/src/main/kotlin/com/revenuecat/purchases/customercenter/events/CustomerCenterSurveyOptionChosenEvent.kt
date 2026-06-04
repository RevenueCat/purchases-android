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
        public val id: UUID = UUID.randomUUID(),
        public val date: Date = Date(),
    )

    @InternalRevenueCatAPI
    @Poko
    @SuppressWarnings("LongParameterList")
    public class Data(
        public val timestamp: Date,
        public val darkMode: Boolean,
        public val locale: String,
        public val version: Int = 1,
        public val revisionID: Int = 1,
        public val displayMode: CustomerCenterDisplayMode = CustomerCenterDisplayMode.FULL_SCREEN,
        public val path: CustomerCenterConfigData.HelpPath.PathType,
        public val url: String?, // URL if CUSTOM_URL
        public val surveyOptionID: String,
        public val additionalContext: String? = null, // null for now until we support

        // isSandbox not available in Android
    ) {
        public val type: CustomerCenterEventType = CustomerCenterEventType.SURVEY_OPTION_CHOSEN
    }
}
