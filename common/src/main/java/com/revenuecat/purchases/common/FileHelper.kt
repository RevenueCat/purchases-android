package com.revenuecat.purchases.common

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class FileHelper(
    private val applicationContext: Context,
) {
    fun appendToFile(filePath: String, contentToAppend: String) {
        val file = getFileInFilesDir(filePath)
        file.parentFile?.mkdirs()
        val outputStream = FileOutputStream(file, true)
        outputStream.write(contentToAppend.toByteArray())
        outputStream.close()
    }

    fun deleteFile(filePath: String): Boolean {
        val file = getFileInFilesDir(filePath)
        return file.delete()
    }

    fun readFilePerLines(filePath: String): List<String> {
        val file = getFileInFilesDir(filePath)
        val fileInputStream = FileInputStream(file)
        var inputStreamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        val readLines = mutableListOf<String>()
        try {
            inputStreamReader = InputStreamReader(fileInputStream)
            bufferedReader = BufferedReader(inputStreamReader)
            readLines.addAll(bufferedReader.readLines())
        } finally {
            bufferedReader?.close()
            inputStreamReader?.close()
            fileInputStream.close()
        }

        return readLines
    }

    fun removeFirstLinesFromFile(filePath: String, numberOfLinesToRemove: Int) {
        val linesToKeep = mutableListOf<String>()

        val file = getFileInFilesDir(filePath)
        val fileInputStream = FileInputStream(file)
        var inputStreamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStreamReader = InputStreamReader(fileInputStream)
            bufferedReader = BufferedReader(inputStreamReader)
            var line: String? = bufferedReader.readLine()
            var lineNumber = 0

            while (line != null) {
                if (lineNumber++ >= numberOfLinesToRemove) {
                    linesToKeep.add(line)
                }
                line = bufferedReader.readLine()
            }
        } finally {
            bufferedReader?.close()
            inputStreamReader?.close()
            fileInputStream.close()
        }

        deleteFile(filePath)
        appendToFile(filePath, linesToKeep.joinToString(separator = ""))
    }

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
