package com.revenuecat.perftester

import java.io.File

internal class ResultsWriter(private val resultsFile: File) {
    fun append(result: PerfResult) {
        resultsFile.parentFile?.mkdirs()
        resultsFile.appendText(result.toJsonLine() + "\n")
    }
}
