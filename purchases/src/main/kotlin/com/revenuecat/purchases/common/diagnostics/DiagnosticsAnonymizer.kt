package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Anonymizer

internal class DiagnosticsAnonymizer(
    private val anonymizer: Anonymizer,
) {
    fun anonymizeEntryIfNeeded(diagnosticsEntry: DiagnosticsEntry): DiagnosticsEntry {
        return when (diagnosticsEntry) {
            is DiagnosticsEntry.Event -> anonymizeEvent(diagnosticsEntry)
            is DiagnosticsEntry.Counter -> anonymizeCounter(diagnosticsEntry)
            is DiagnosticsEntry.Histogram -> diagnosticsEntry
        }
    }

    private fun anonymizeEvent(diagnosticsEvent: DiagnosticsEntry.Event): DiagnosticsEntry {
        return diagnosticsEvent.copy(
            properties = anonymizer.anonymizedMap(diagnosticsEvent.properties),
        )
    }

    private fun anonymizeCounter(diagnosticsCounter: DiagnosticsEntry.Counter): DiagnosticsEntry {
        return diagnosticsCounter.copy(
            tags = anonymizer.anonymizedStringMap(diagnosticsCounter.tags),
        )
    }
}
