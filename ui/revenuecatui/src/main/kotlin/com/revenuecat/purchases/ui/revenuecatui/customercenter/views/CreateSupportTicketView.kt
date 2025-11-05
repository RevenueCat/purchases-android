@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants.Layout.SECTION_SPACING
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CreateSupportTicketData

@JvmSynthetic
@Composable
internal fun CreateSupportTicketView(
    data: CreateSupportTicketData,
    modifier: Modifier = Modifier,
    localization: CustomerCenterConfigData.Localization,
) {
    var email by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = localization.commonLocalizedString(
        CustomerCenterConfigData.Localization.CommonLocalizedString.SUPPORT_TICKET_FAILED,
    )

    LaunchedEffect(hasError) {
        if (hasError) {
            snackbarHostState.showSnackbar(errorMessage)
            hasError = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SECTION_SPACING)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.EMAIL,
                )) },
                placeholder = { Text(localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.ENTER_EMAIL,
                )) },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_field"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
            )

            Spacer(modifier = Modifier.height(SECTION_TITLE_BOTTOM_PADDING))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.DESCRIPTION,
                )) },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("description_field"),
                minLines = 6,
                maxLines = 10,
            )

            Spacer(modifier = Modifier.height(SECTION_SPACING))

            SettingsButton(
                onClick = {
                    isSubmitting = true
                    hasError = false
                    data.onSubmit(
                        email,
                        description,
                        {
                            // Success - navigation handled by ViewModel
                        },
                        {
                            isSubmitting = false
                            hasError = true
                        }
                    )
                },
                enabled = !isSubmitting && email.isNotBlank() && description.isNotBlank(),
                loading = isSubmitting,
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET,
                ),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(SECTION_SPACING),
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

