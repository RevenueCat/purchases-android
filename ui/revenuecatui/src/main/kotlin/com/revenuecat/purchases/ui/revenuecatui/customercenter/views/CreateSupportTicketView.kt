@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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
    initialEmail: String = "",
    initialDescription: String = "",
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var emailDirty by rememberSaveable { mutableStateOf(false) }
    var emailHasFocus by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var hasError by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        CreateSupportTicketContent(
            emailState = EmailInputState(
                email = email,
                onEmailChange = {
                    email = it
                    if (!emailDirty) emailDirty = true
                },
                onFocusChanged = { emailHasFocus = it },
                showError = emailDirty && !emailHasFocus && !isValidEmail(email),
                enabled = !isSubmitting,
            ),
            descriptionState = DescriptionInputState(
                description = description,
                onDescriptionChange = { newValue ->
                    if (newValue.length <= MAX_DESCRIPTION_LENGTH) {
                        description = newValue
                    }
                },
                enabled = !isSubmitting,
                showDone = !isSubmitting && isValidEmail(email.trim()),
                onSubmit = {
                    isSubmitting = true
                    hasError = false
                    data.onSubmit(
                        email.trim(),
                        description,
                        { /* Success - navigation handled by ViewModel */ },
                        {
                            isSubmitting = false
                            hasError = true
                        },
                    )
                },
            ),
            isSubmitting = isSubmitting,
            localization = localization,
        )

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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun CreateSupportTicketView_WithDataPreview() {
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
                initialEmail = "user@example.com",
                initialDescription = "I'm having an issue with my subscription.",
            )
        }
    }
}

private const val MAX_DESCRIPTION_LENGTH = 250

private fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private data class EmailInputState(
    val email: String,
    val onEmailChange: (String) -> Unit,
    val onFocusChanged: (Boolean) -> Unit,
    val showError: Boolean,
    val enabled: Boolean,
)

private data class DescriptionInputState(
    val description: String,
    val onDescriptionChange: (String) -> Unit,
    val enabled: Boolean,
    val showDone: Boolean = false,
    val onSubmit: () -> Unit = {},
)

@Composable
private fun CreateSupportTicketContent(
    emailState: EmailInputState,
    descriptionState: DescriptionInputState,
    isSubmitting: Boolean,
    localization: CustomerCenterConfigData.Localization,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SECTION_SPACING)
            .verticalScroll(rememberScrollState()),
    ) {
        EmailInputField(
            state = emailState,
            localization = localization,
            focusManager = LocalFocusManager.current,
        )

        Spacer(modifier = Modifier.height(SECTION_TITLE_BOTTOM_PADDING))

        DescriptionInputField(
            state = descriptionState,
            localization = localization,
        )

        Spacer(modifier = Modifier.height(SECTION_SPACING))

        SubmitTicketButton(
            email = emailState.email,
            description = descriptionState.description,
            isSubmitting = isSubmitting,
            onSubmit = descriptionState.onSubmit,
            localization = localization,
        )
    }
}

@Composable
private fun EmailInputField(
    state: EmailInputState,
    localization: CustomerCenterConfigData.Localization,
    focusManager: FocusManager,
) {
    OutlinedTextField(
        value = state.email,
        onValueChange = state.onEmailChange,
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
        isError = state.showError,
        supportingText = if (state.showError) {
            {
                Text(
                    localization.commonLocalizedString(
                        CustomerCenterConfigData.Localization.CommonLocalizedString.INVALID_EMAIL_ERROR,
                    ),
                )
            }
        } else {
            null
        },
        enabled = state.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                state.onFocusChanged(focusState.isFocused)
            }
            .testTag("email_field"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
    )
}

@Composable
private fun DescriptionInputField(
    state: DescriptionInputState,
    localization: CustomerCenterConfigData.Localization,
) {
    val usedChars = state.description.length
    val charsText = "$usedChars / $MAX_DESCRIPTION_LENGTH"
    val remainingCharsText = localization.commonLocalizedString(
        CustomerCenterConfigData.Localization.CommonLocalizedString.CHARACTERS_REMAINING,
    ).replace("{{ count }}", charsText)

    val currentOnSubmit by rememberUpdatedState(state.onSubmit)

    OutlinedTextField(
        value = state.description,
        onValueChange = state.onDescriptionChange,
        label = {
            Text(
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.DESCRIPTION,
                ),
            )
        },
        supportingText = {
            Text(
                text = remainingCharsText,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        enabled = state.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("description_field"),
        minLines = 6,
        maxLines = 10,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (state.showDone && state.description.isNotBlank()) currentOnSubmit() },
        ),
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
            enabled = !isSubmitting && isValidEmail(email) && description.isNotBlank(),
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
    val currentOnErrorShow by rememberUpdatedState(onErrorShow)

    LaunchedEffect(hasError) {
        if (hasError) {
            errorSnackbarHostState.showSnackbar(errorMessage)
            currentOnErrorShow()
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
