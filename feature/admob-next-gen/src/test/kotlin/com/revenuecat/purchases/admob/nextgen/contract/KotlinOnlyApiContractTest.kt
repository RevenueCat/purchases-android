package com.revenuecat.purchases.admob.nextgen.contract

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinOnlyApiContractTest {

    @Test
    fun `all public helper methods are Kotlin only`() {
        val apiFile = findApiFile()
        val publicMethods = apiFile.useLines { lines ->
            lines
                .map(String::trim)
                .filter { it.startsWith("method ") }
                .toList()
        }

        assertTrue("Expected public methods in ${apiFile.path}", publicMethods.isNotEmpty())
        publicMethods.forEach { method ->
            assertTrue("Expected @KotlinOnly on: $method", method.contains("@KotlinOnly"))
            assertTrue(
                "Expected @JvmSynthetic on: $method",
                method.contains("@kotlin.jvm.JvmSynthetic"),
            )
        }
    }

    private fun findApiFile(): File {
        val candidates = listOf(
            File("api.txt"),
            File("feature/admob-next-gen/api.txt"),
        )
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not find feature/admob-next-gen/api.txt from ${File(".").absolutePath}")
    }
}
