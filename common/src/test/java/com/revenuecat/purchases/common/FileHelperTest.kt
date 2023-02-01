package com.revenuecat.purchases.common

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FileHelperTest {

    private val testFolder = "temp_test_folder"
    private val testFilePath = "RevenueCat/test_file.txt"

    private lateinit var applicationContext: Context

    private lateinit var fileHelper: FileHelper

    @Before
    fun setup() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        applicationContext = mockk()
        every { applicationContext.filesDir } returns tempTestFolder

        fileHelper = FileHelper(applicationContext)
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @Test
    fun `appendToFile creates file if it does not exist`() {
        verifyFileDoesNotExist(testFilePath)
        fileHelper.appendToFile(testFilePath, "test string")
        verifyFileExistsWithContents(testFilePath, "test string")
    }

    @Test
    fun `appendToFile appends to file if it exists with content`() {
        createTestFileWithContents(testFilePath, "test string 1")
        fileHelper.appendToFile(testFilePath, "\ntest string 2")
        verifyFileExistsWithContents(testFilePath, "test string 1\ntest string 2")
    }

    @Test
    fun `deleteFile calls appropriate methods`() {
        createTestFileWithContents(testFilePath, "")
        fileHelper.deleteFile(testFilePath)
        verifyFileDoesNotExist(testFilePath)
    }

    @Test
    fun `removeFirstLinesFromFile leaves content after first lines`() {
        createTestFileWithContents(testFilePath, "first line\nsecond line\nthird line\nfourth line")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents(testFilePath, "fourth line")
    }

    @Test
    fun `removeFirstLinesFromFile leaves empty file if exact lines removed`() {
        createTestFileWithContents(testFilePath, "first line\nsecond line\nthird line")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents(testFilePath, "")
    }

    @Test
    fun `removeFirstLinesFromFile leaves empty file if fewer removed`() {
        createTestFileWithContents(testFilePath, "first line\nsecond line")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents(testFilePath, "")
    }

    @Test
    fun `fileIsEmpty returns true if file exists and is empty`() {
        createTestFileWithContents(testFilePath, "")
        val fileIsEmpty = fileHelper.fileIsEmpty(testFilePath)
        assertThat(fileIsEmpty).isTrue
    }

    @Test
    fun `fileIsEmpty returns false if file exists and has text`() {
        createTestFileWithContents(testFilePath, "a")
        val fileIsEmpty = fileHelper.fileIsEmpty(testFilePath)
        assertThat(fileIsEmpty).isFalse
    }

    @Test
    fun `fileIsEmpty returns true if file does not exist`() {
        verifyFileDoesNotExist(testFilePath)
        val fileIsEmpty = fileHelper.fileIsEmpty(testFilePath)
        assertThat(fileIsEmpty).isTrue
    }

    private fun verifyFileDoesNotExist(filePath: String) {
        val file = File(testFolder, filePath)
        if (file.exists()) {
            fail<Unit>("File $file should not exist")
        }
    }

    private fun verifyFileExistsWithContents(filePath: String, expectedContents: String) {
        val file = File(testFolder, filePath)
        if (!file.exists()) {
            fail<Unit>("File $file should exist")
        }
        val contents = file.readText()
        assertThat(contents).isEqualTo(expectedContents)
    }

    private fun createTestFileWithContents(filePath: String, contents: String) {
        val file = File(testFolder, filePath)
        file.parentFile?.mkdirs()
        file.createNewFile()
        file.writeText(contents)
    }
}
