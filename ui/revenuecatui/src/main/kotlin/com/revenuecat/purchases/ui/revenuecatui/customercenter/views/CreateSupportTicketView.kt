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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants.Layout.SECTION_SPACING
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CreateSupportTicketData

@JvmSynthetic
@Composable
internal fun CreateSupportTicketView(
    data: CreateSupportTicketData,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var wasSuccessfullySent by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

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
                label = { Text("Email") },
                placeholder = { Text("Enter your email") },
                enabled = !isSubmitting && !wasSuccessfullySent,
                modifier = Modifier.fillMaxWidth(),
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
                label = { Text("Description") },
                placeholder = { Text("Enter ticket description") },
                enabled = !isSubmitting && !wasSuccessfullySent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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
                            isSubmitting = false
                            wasSuccessfullySent = true
                        },
                        {
                            isSubmitting = false
                            hasError = true
                        }
                    )
                },
                enabled = !isSubmitting && !wasSuccessfullySent && email.isNotBlank() && description.isNotBlank(),
                loading = isSubmitting,
                title = if (wasSuccessfullySent) "✓ Sent" else "Submit ticket",
            )

            if (hasError) {
                Spacer(modifier = Modifier.height(SECTION_SPACING))
                Text(
                    text = "Failed to send support ticket. Please try again.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SECTION_TITLE_BOTTOM_PADDING),
                )
            }
        }
    }
}

@JvmSynthetic
@Preview(showBackground = true)
@Composable
internal fun CreateSupportTicketViewPreview() {
    CreateSupportTicketView(
        data = CreateSupportTicketData(
            onSubmit = { _, _, _, _ -> },
            onCancel = { },
            onClose = { },
        ),
    )
}
