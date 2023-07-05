package com.revenuecat.purchases.common

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

internal class FileHelper(
    private val applicationContext: Context,
) {
    fun appendToFile(filePath: String, contentToAppend: String) {
        val file = getFileInFilesDir(filePath)
        file.parentFile?.mkdirs()
        val shouldAppend = true
        val outputStream = FileOutputStream(file, shouldAppend)
        outputStream.use {
            outputStream.write(contentToAppend.toByteArray())
        }
    }

    fun deleteFile(filePath: String): Boolean {
        val file = getFileInFilesDir(filePath)
        return file.delete()
    }

    fun readFilePerLines(filePath: String): List<String> {
        val readLines = mutableListOf<String>()
        val file = getFileInFilesDir(filePath)
        FileInputStream(file).use { fileInputStream ->
            InputStreamReader(fileInputStream).use { inputStreamReader ->
                BufferedReader(inputStreamReader).use { bufferedReader ->
                    readLines.addAll(bufferedReader.readLines())
                }
            }
        }
        return readLines
    }

    fun removeFirstLinesFromFile(filePath: String, numberOfLinesToRemove: Int) {
        val readLines = readFilePerLines(filePath)
        deleteFile(filePath)
        val textToAppend = if (readLines.isEmpty() || numberOfLinesToRemove >= readLines.size) {
            errorLog("Trying to remove $numberOfLinesToRemove from file with ${readLines.size} lines.")
            ""
        } else {
            readLines.subList(numberOfLinesToRemove, readLines.size).joinToString(separator = "\n", postfix = "\n")
        }
        appendToFile(filePath, textToAppend)
    }

    /**
     * This will return true if file does not exist or its contents are empty.
     */
    fun fileIsEmpty(filePath: String): Boolean {
        val file = getFileInFilesDir(filePath)
        return !file.exists() || file.length() == 0L
    }

    private fun getFileInFilesDir(filePath: String): File {
        return File(getFilesDir(), filePath)
    }

    private fun getFilesDir(): File {
        return applicationContext.filesDir
    }
}
