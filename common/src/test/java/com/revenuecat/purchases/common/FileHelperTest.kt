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
        verifyFileDoesNotExist()
        fileHelper.appendToFile(testFilePath, "test string")
        verifyFileExistsWithContents("test string")
    }

    @Test
    fun `appendToFile appends to file if it exists with content`() {
        createTestFileWithContents("test string 1")
        fileHelper.appendToFile(testFilePath, "\ntest string 2")
        verifyFileExistsWithContents("test string 1\ntest string 2")
    }

    @Test
    fun `deleteFile calls appropriate methods`() {
        createTestFileWithContents("")
        fileHelper.deleteFile(testFilePath)
        verifyFileDoesNotExist()
    }

    @Test
    fun `removeFirstLinesFromFile leaves multiple lines after first lines`() {
        createTestFileWithContents("first line\nsecond line\nthird line\nfourth line\nfifth line\n")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents("fourth line\nfifth line\n")
    }

    @Test
    fun `removeFirstLinesFromFile leaves content after first lines`() {
        createTestFileWithContents("first line\nsecond line\nthird line\nfourth line\n")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents("fourth line\n")
    }

    @Test
    fun `removeFirstLinesFromFile leaves empty file if exact lines removed`() {
        createTestFileWithContents("first line\nsecond line\nthird line")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents("")
    }

    @Test
    fun `removeFirstLinesFromFile leaves empty file if fewer removed`() {
        createTestFileWithContents("first line\nsecond line")
        fileHelper.removeFirstLinesFromFile(testFilePath, 3)
        verifyFileExistsWithContents("")
    }

    @Test
    fun `fileIsEmpty returns true if file exists and is empty`() {
        createTestFileWithContents("")
        val fileIsEmpty = fileHelper.fileIsEmpty(testFilePath)
        assertThat(fileIsEmpty).isTrue
    }

    @Test
    fun `fileIsEmpty returns false if file exists and has text`() {
        createTestFileWithContents("a")
        val fileIsEmpty = fileHelper.fileIsEmpty(testFilePath)
        assertThat(fileIsEmpty).isFalse
    }

    @Test
    fun `fileIsEmpty returns true if file does not exist`() {
        verifyFileDoesNotExist()
        val fileIsEmpty = fileHelper.fileIsEmpty(testFilePath)
        assertThat(fileIsEmpty).isTrue
    }

    @Test
    fun `readFilePerLines returns correct content`() {
        createTestFileWithContents("first line\nsecond line\nthird line\nfourth line")
        val filePerLines = fileHelper.readFilePerLines(testFilePath)
        assertThat(filePerLines).isEqualTo(listOf("first line", "second line", "third line", "fourth line"))
    }

    private fun verifyFileDoesNotExist() {
        val file = File(testFolder, testFilePath)
        if (file.exists()) {
            fail<Unit>("File $file should not exist")
        }
    }

    private fun verifyFileExistsWithContents(expectedContents: String) {
        val file = File(testFolder, testFilePath)
        if (!file.exists()) {
            fail<Unit>("File $file should exist")
        }
        val contents = file.readText()
        assertThat(contents).isEqualTo(expectedContents)
    }

    private fun createTestFileWithContents(contents: String) {
        val file = File(testFolder, testFilePath)
        file.parentFile?.mkdirs()
        file.createNewFile()
        file.writeText(contents)
    }
}
