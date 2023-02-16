package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Anonymizer

class DiagnosticsAnonymizer(
    private val anonymizer: Anonymizer
) {
    fun anonymizeEntryIfNeeded(diagnosticsEntry: DiagnosticsEntry): DiagnosticsEntry {
        return when (diagnosticsEntry) {
            is DiagnosticsEntry.Event -> anonymizeEvent(diagnosticsEntry)
            is DiagnosticsEntry.Metric -> diagnosticsEntry
        }
    }

    private fun anonymizeEvent(diagnosticsEvent: DiagnosticsEntry.Event): DiagnosticsEntry {
        return diagnosticsEvent.copy(
            properties = anonymizer.anonymizedMap(diagnosticsEvent.properties)
        )
    }
}
