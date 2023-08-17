package com.revenuecat.purchases.common

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.utils.sizeInKB
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.stream.Stream

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

    // This is using a lambda with a Stream instead of returning the Stream itself. This is so we keep
    // the responsibility of closing the bufferedReader to this class. Note that the Stream should
    // be used synchronously, otherwise the bufferedReader will be closed before the stream is used.
    @RequiresApi(Build.VERSION_CODES.N)
    fun readFilePerLines(filePath: String, streamBlock: ((Stream<String>) -> Unit)) {
        openBufferedReader(filePath) { bufferedReader ->
            streamBlock(bufferedReader.lines())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun removeFirstLinesFromFile(filePath: String, numberOfLinesToRemove: Int) {
        val textToAppend = StringBuilder()
        readFilePerLines(filePath) { stream ->
            stream.skip(numberOfLinesToRemove.toLong()).forEach { line ->
                textToAppend.append(line).append("\n")
            }
        }
        deleteFile(filePath)
        appendToFile(filePath, textToAppend.toString())
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
