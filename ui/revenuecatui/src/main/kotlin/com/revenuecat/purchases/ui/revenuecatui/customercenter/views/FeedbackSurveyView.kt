@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData

@JvmSynthetic
@Composable
internal fun FeedbackSurveyView(
    data: FeedbackSurveyData,
) {
    var loadingOption by remember { mutableStateOf<String?>(null) }
    val feedbackSurvey = data.feedbackSurvey

    Box {
        FeedbackSurveyButtonsView(
            options = feedbackSurvey.options,
            onAnswerSubmit = { option ->
                loadingOption = option.id
                data.onAnswerSubmitted(option)
                loadingOption = null
            },
            loadingOption = loadingOption,
        )
    }
}

@JvmSynthetic
@Composable
internal fun FeedbackSurveyButtonsView(
    options: List<CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option>,
    onAnswerSubmit: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option) -> Unit,
    loadingOption: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        options.forEach { option ->
            Button(
                onClick = { onAnswerSubmit(option) },
                enabled = loadingOption == null,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            ) {
                if (loadingOption == option.id) {
                    CircularProgressIndicator()
                } else {
                    Text(option.title)
                }
            }
        }
    }
}

@JvmSynthetic
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true)
@Composable
internal fun FeedbackSurveyPreview() {
    FeedbackSurveyView(
        FeedbackSurveyData(
            feedbackSurvey = CustomerCenterConfigTestData.customerCenterData()
                .getManagementScreen()?.paths?.first { it.type == CustomerCenterConfigData.HelpPath.PathType.CANCEL }!!
                .feedbackSurvey!!,
            onAnswerSubmitted = { _ -> },
        ),
    )
}
