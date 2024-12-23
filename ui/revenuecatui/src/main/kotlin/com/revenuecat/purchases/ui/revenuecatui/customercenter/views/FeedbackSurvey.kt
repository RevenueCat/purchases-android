@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

private typealias OptionDetails = CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSurvey(
    path: CustomerCenterConfigData.HelpPath,
    title: String,
    onOptionSelected: (OptionDetails, () -> Unit) -> Unit,
) {
    var loadingOption by remember { mutableStateOf<String?>(null) }
    val feedbackSurvey = path.feedbackSurvey ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            FeedbackSurveyButtonsView(
                options = feedbackSurvey.options,
                onOptionSelected = { option ->
                    loadingOption = option.id
                    onOptionSelected(option) {
                        loadingOption = null // Reset loading state after action is complete
                    }
                },
                loadingOption = loadingOption,
            )
        }
    }
}

@Composable
fun FeedbackSurveyButtonsView(
    options: List<CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option>,
    onOptionSelected: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option) -> Unit,
    loadingOption: String?,
) {
    Column {
        options.forEach { option ->
            Button(
                onClick = { onOptionSelected(option) },
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

@Preview(showBackground = true)
@Composable
fun FeedbackSurveyPreview() {
    FeedbackSurvey(
        path = CustomerCenterConfigTestData.customerCenterData()
            .getManagementScreen()?.paths?.first { it.type == CustomerCenterConfigData.HelpPath.PathType.CANCEL }!!,
        onOptionSelected = { _, _ -> },
        title = "Why are you cancelling?",
    )
}
