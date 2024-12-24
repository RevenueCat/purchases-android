@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.FeedbackSurveyData

@OptIn(ExperimentalMaterial3Api::class)
@JvmSynthetic
@Composable
internal fun FeedbackSurvey(
    data: FeedbackSurveyData,
    modifier: Modifier = Modifier,
) {
    var loadingOption by remember { mutableStateOf<String?>(null) }
    val feedbackSurvey = data.path.feedbackSurvey ?: return
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                // Had to add this to prevent a padding  that appears on top
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp,
                ),
                title = {
                    Text(
                        feedbackSurvey.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        data.onOptionSelected(null)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            FeedbackSurveyButtonsView(
                options = feedbackSurvey.options,
                onOptionSelect = { option ->
                    loadingOption = option.id
                    data.onOptionSelected(option)
                    loadingOption = null
                },
                loadingOption = loadingOption,
            )
        }
    }
}

@JvmSynthetic
@Composable
internal fun FeedbackSurveyButtonsView(
    options: List<CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option>,
    onOptionSelect: (CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option) -> Unit,
    loadingOption: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        options.forEach { option ->
            Button(
                onClick = { onOptionSelect(option) },
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
    FeedbackSurvey(
        FeedbackSurveyData(
            path = CustomerCenterConfigTestData.customerCenterData()
                .getManagementScreen()?.paths?.first { it.type == CustomerCenterConfigData.HelpPath.PathType.CANCEL }!!,
            onOptionSelected = { _ -> },
        ),

    )
}
