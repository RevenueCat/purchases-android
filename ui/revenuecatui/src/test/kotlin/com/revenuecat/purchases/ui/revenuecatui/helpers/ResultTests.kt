package com.revenuecat.purchases.ui.revenuecatui.helpers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ResultTests {

    @Test
    fun `zipOrAccumulate with two successful results should return transformed success`() {
        // Arrange
        val result1: Result<Int, NonEmptyList<*>> = Result.Success(1)
        val result2: Result<String, NonEmptyList<*>> = Result.Success("2")

        // Act
        val actual = zipOrAccumulate(
            first = result1,
            second = result2,
        ) { num, str -> "$num$str" }

        // Assert
        assertThat(actual).isInstanceOf(Result.Success::class.java)
        assertThat(actual.getOrNull()).isEqualTo("12")
    }

    @Test
    fun `zipOrAccumulate should accumulate errors from multiple results`() {
        // Arrange
        val result1: Result<String, NonEmptyList<String>> = Result.Error(nonEmptyListOf("error1"))
        val result2: Result<String, NonEmptyList<String>> = Result.Error(nonEmptyListOf("error2"))

        // Act
        val actual = zipOrAccumulate(result1, result2) { a, b -> "$a$b" }

        // Assert
        assertThat(actual).isInstanceOf(Result.Error::class.java)
        assertThat(actual.errorOrNull()?.toList()).containsExactly("error1", "error2")
    }

    @Test
    fun `mapOrAccumulate should transform a list of successful results`() {
        // Arrange
        val results = listOf<Result<Int, NonEmptyList<Int>>>(
            Result.Success(1),
            Result.Success(2),
            Result.Success(3),
        )

        // Act
        val actual = results.mapOrAccumulate { it * 2 }

        // Assert
        assertThat(actual).isInstanceOf(Result.Success::class.java)
        assertThat(actual.getOrNull()).containsExactly(2, 4, 6)
    }

    @Test
    fun `mapOrAccumulate should accumulate errors from multiple results`() {
        // Arrange
        val results = listOf(
            Result.Success(1),
            Result.Error(nonEmptyListOf("error1")),
            Result.Error(nonEmptyListOf("error2"))
        )

        // Act
        val actual = results.mapOrAccumulate { it * 2 }

        // Assert
        assertThat(actual).isInstanceOf(Result.Error::class.java)
        assertThat(actual.errorOrNull()?.toList()).containsExactly("error1", "error2")
    }

    @Test
    fun `mapOrAccumulate should return an empty list for empty input`() {
        // Arrange
        val results = emptyList<Result<Int, NonEmptyList<String>>>()


        // Act
        val actual = results.mapOrAccumulate { it * 2 }

        // Assert
        assertThat(actual).isInstanceOf(Result.Success::class.java)
        assertThat(actual.getOrNull()).isEmpty()
    }
    
}
