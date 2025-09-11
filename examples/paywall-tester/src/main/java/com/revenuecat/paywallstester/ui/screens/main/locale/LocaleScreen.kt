package com.revenuecat.paywallstester.ui.screens.main.locale

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.Purchases

private const val MESSAGE_HIDE_DELAY = 4000L
private const val LOG_DELAY = 100L

@Suppress("LongMethod")
@Composable
fun LocaleScreen(
    modifier: Modifier = Modifier,
) {
    // Initialize selectedLocale with the current preferred locale override
    val currentPreferredLocale = Purchases.sharedInstance.preferredUILocaleOverride
    var selectedLocale by remember { mutableStateOf(currentPreferredLocale) }

    val commonLocales = listOf(
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "es-ES" to "Spanish (Spain)",
        "es-MX" to "Spanish (Mexico)",
        "fr-FR" to "French (France)",
        "de-DE" to "German",
        "it-IT" to "Italian",
        "pt-BR" to "Portuguese (Brazil)",
        "ja-JP" to "Japanese",
        "ko-KR" to "Korean",
        "zh-CN" to "Chinese (Simplified)",
        "zh-TW" to "Chinese (Traditional)",
        "ru-RU" to "Russian",
        "ar-SA" to "Arabic",
        "hi-IN" to "Hindi",
    )

    // If current locale is custom (not in predefined list), initialize custom input
    var customLocaleInput by remember {
        mutableStateOf(
            if (currentPreferredLocale != null && !commonLocales.any { it.first == currentPreferredLocale }) {
                currentPreferredLocale
            } else {
                ""
            },
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Preferred UI Locale Override",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Current: ${Purchases.sharedInstance.preferredUILocaleOverride ?: "System default"}",
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selectedLocale == null,
                onClick = { selectedLocale = null },
            )
            Text("System default")
        }

        Divider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(commonLocales) { (localeCode, displayName) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLocale = localeCode },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedLocale == localeCode,
                        onClick = { selectedLocale = localeCode },
                    )
                    Column {
                        Text(displayName)
                        Text(
                            text = localeCode,
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Divider()

        Text("Custom Locale:", modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = customLocaleInput,
            onValueChange = { customLocaleInput = it },
            label = { Text("e.g. en-US, pt-BR") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selectedLocale == customLocaleInput && customLocaleInput.isNotEmpty(),
                onClick = {
                    if (customLocaleInput.isNotEmpty()) {
                        selectedLocale = customLocaleInput
                    }
                },
            )
            Text("Use custom locale")
        }

        var statusMessage by remember { mutableStateOf<String?>(null) }
        var isError by remember { mutableStateOf(false) }

        statusMessage?.let { message ->
            Text(
                text = message,
                color = if (isError) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                Purchases.sharedInstance.overridePreferredUILocale(selectedLocale)

                // Hide message after 4 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    statusMessage = null
                }, MESSAGE_HIDE_DELAY)

                // Log for debugging
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentOverride = Purchases.sharedInstance.preferredUILocaleOverride
                    Log.d(
                        "LocaleScreen",
                        "Applied locale override: $selectedLocale, " +
                            "current value: $currentOverride",
                    )
                }, LOG_DELAY)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text("Apply Locale Override")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocaleScreenPreview() {
    LocaleScreen()
}
