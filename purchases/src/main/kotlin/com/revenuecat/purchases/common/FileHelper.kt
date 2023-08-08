package com.revenuecat.purchases.common

import android.content.Context
import com.revenuecat.purchases.utils.DataListener
import com.revenuecat.purchases.utils.sizeInKB
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

internal class FileHelper(
    private val applicationContext: Context,
) {
    fun fileSizeInKB(filePath: String): Double {
        val file = getFileInFilesDir(filePath)
        return file.sizeInKB
    }

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

    fun readFilePerLines(filePath: String, dataListener: DataListener<Pair<String, Int>>) {
        openBufferedReader(filePath) { bufferedReader ->
            var nextLine: String? = bufferedReader.readLine()
            var lineNumber = 0
            while (nextLine != null) {
                dataListener.onData(Pair(nextLine, lineNumber))
                nextLine = bufferedReader.readLine()
                lineNumber++
            }
        }
        dataListener.onComplete()
    }

    fun removeFirstLinesFromFile(filePath: String, numberOfLinesToRemove: Int) {
        val textToAppend = StringBuilder()
        readFilePerLines(
            filePath,
            object : DataListener<Pair<String, Int>> {
                override fun onData(data: Pair<String, Int>) {
                    if (data.second >= numberOfLinesToRemove) {
                        textToAppend.append(data.first).append("\n")
                    }
                }

                override fun onComplete() {
                    deleteFile(filePath)
                    appendToFile(filePath, textToAppend.toString())
                }
            },
        )
    }

    /**
     * This will return true if file does not exist or its contents are empty.
     */
    fun fileIsEmpty(filePath: String): Boolean {
        val file = getFileInFilesDir(filePath)
        return !file.exists() || file.length() == 0L
    }

    private fun openBufferedReader(filePath: String, contentBlock: ((BufferedReader) -> Unit)) {
        val file = getFileInFilesDir(filePath)
        FileInputStream(file).use { fileInputStream ->
            InputStreamReader(fileInputStream).use { inputStreamReader ->
                BufferedReader(inputStreamReader).use { bufferedReader ->
                    contentBlock(bufferedReader)
                }
            }
        }
    }

    private fun getFileInFilesDir(filePath: String): File {
        return File(getFilesDir(), filePath)
    }

    private fun getFilesDir(): File {
        return applicationContext.filesDir
    }
}
