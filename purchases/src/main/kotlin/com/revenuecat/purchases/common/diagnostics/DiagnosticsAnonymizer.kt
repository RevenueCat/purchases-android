package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Anonymizer

internal class DiagnosticsAnonymizer(
    private val anonymizer: Anonymizer,
) {
    fun anonymizeEntryIfNeeded(diagnosticsEntry: DiagnosticsEntry): DiagnosticsEntry {
        return diagnosticsEntry.copy(
            properties = anonymizer.anonymizedMap(diagnosticsEntry.properties),
        )
    }
}
