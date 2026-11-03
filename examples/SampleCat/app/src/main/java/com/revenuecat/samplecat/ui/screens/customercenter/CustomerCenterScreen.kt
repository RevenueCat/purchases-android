package com.revenuecat.samplecat.ui.screens.customercenter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.ui.components.ConceptIntroduction
import com.revenuecat.samplecat.ui.components.ContentBackground
import com.revenuecat.samplecat.ui.components.NavigationRowCard
import com.revenuecat.samplecat.ui.theme.RCPurple

/**
 * Screen displaying the Customer Center entry point.
 */
@Composable
fun CustomerCenterScreen(
    modifier: Modifier = Modifier,
) {
    var showCustomerCenter by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        ContentBackground(color = RCPurple)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                ConceptIntroduction(
                    imageRes = R.drawable.visual_customer_center,
                    title = stringResource(R.string.customer_center_title),
                    description = stringResource(R.string.customer_center_description),
                )
            }

            item {
                NavigationRowCard(
                    title = stringResource(R.string.customer_center_open),
                    onClick = { showCustomerCenter = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }

    // Show Customer Center dialog
    if (showCustomerCenter) {
        Dialog(
            onDismissRequest = { showCustomerCenter = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            CustomerCenter(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showCustomerCenter = false },
            )
        }
    }
}
