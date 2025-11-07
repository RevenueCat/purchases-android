@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonConfig
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CreateSupportTicketData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@JvmSynthetic
@Composable
internal fun CreateSupportTicketView(
    data: CreateSupportTicketData,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SECTION_SPACING)
                .verticalScroll(rememberScrollState()),
        ) {
            EmailInputField(
                email = email,
                onEmailChange = { email = it },
                enabled = !isSubmitting,
                localization = localization,
            )

            Spacer(modifier = Modifier.height(SECTION_TITLE_BOTTOM_PADDING))

            DescriptionInputField(
                description = description,
                onDescriptionChange = { description = it },
                enabled = !isSubmitting,
                localization = localization,
            )

            Spacer(modifier = Modifier.height(SECTION_SPACING))

            SubmitTicketButton(
                email = email,
                description = description,
                isSubmitting = isSubmitting,
                onSubmit = {
                    isSubmitting = true
                    hasError = false
                    data.onSubmit(
                        email,
                        description,
                        { /* Success - navigation handled by ViewModel */ },
                        {
                            isSubmitting = false
                            hasError = true
                        },
                    )
                },
                localization = localization,
            )
        }

        ErrorSnackbar(
            hasError = hasError,
            onErrorShow = { hasError = false },
            localization = localization,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun CreateSupportTicketView_Preview() {
    val mockData = CreateSupportTicketData(
        onSubmit = { _, _, _, _ -> },
        onCancel = { },
        onClose = { },
    )

    CustomerCenterPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            CreateSupportTicketView(
                mockData,
                CustomerCenterConfigTestData.customerCenterData().localization,
            )
        }
    }
}


@Composable
private fun EmailInputField(
    email: String,
    onEmailChange: (String) -> Unit,
    enabled: Boolean,
    localization: CustomerCenterConfigData.Localization,
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = {
            Text(
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.EMAIL,
                ),
            )
        },
        placeholder = {
            Text(
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.ENTER_EMAIL,
                ),
            )
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_field"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
private fun DescriptionInputField(
    description: String,
    onDescriptionChange: (String) -> Unit,
    enabled: Boolean,
    localization: CustomerCenterConfigData.Localization,
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = {
            Text(
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.DESCRIPTION,
                ),
            )
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("description_field"),
        minLines = 6,
        maxLines = 10,
    )
}

@Composable
private fun SubmitTicketButton(
    email: String,
    description: String,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    localization: CustomerCenterConfigData.Localization,
) {
    SettingsButton(
        onClick = onSubmit,
        config = SettingsButtonConfig(
            enabled = !isSubmitting && email.isNotBlank() && description.isNotBlank(),
            loading = isSubmitting,
        ),
        title = localization.commonLocalizedString(
            CustomerCenterConfigData.Localization.CommonLocalizedString.SUBMIT_TICKET,
        ),
    )
}

@Composable
private fun ErrorSnackbar(
    hasError: Boolean,
    onErrorShow: () -> Unit,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    val errorSnackbarHostState = remember { SnackbarHostState() }
    val errorMessage = localization.commonLocalizedString(
        CustomerCenterConfigData.Localization.CommonLocalizedString.SUPPORT_TICKET_FAILED,
    )

    LaunchedEffect(hasError, onErrorShow) {
        if (hasError) {
            errorSnackbarHostState.showSnackbar(errorMessage)
            onErrorShow()
        }
    }

    SnackbarHost(
        hostState = errorSnackbarHostState,
        modifier = modifier.padding(SECTION_SPACING),
    ) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
