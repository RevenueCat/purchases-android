package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.common.OfferingParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class VariableLocalizationKeyTests(
    private val serialized: String,
    private val expected: VariableLocalizationKey,
) {

    companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = VariableLocalizationKey.values().map { expected ->
            val serialized = when (expected) {
                VariableLocalizationKey.DAY -> "\"day\""
                VariableLocalizationKey.DAILY -> "\"daily\""
                VariableLocalizationKey.DAY_SHORT -> "\"day_short\""
                VariableLocalizationKey.WEEK -> "\"week\""
                VariableLocalizationKey.WEEKLY -> "\"weekly\""
                VariableLocalizationKey.WEEK_SHORT -> "\"week_short\""
                VariableLocalizationKey.MONTH -> "\"month\""
                VariableLocalizationKey.MONTHLY -> "\"monthly\""
                VariableLocalizationKey.MONTH_SHORT -> "\"month_short\""
                VariableLocalizationKey.YEAR -> "\"year\""
                VariableLocalizationKey.YEARLY -> "\"yearly\""
                VariableLocalizationKey.YEAR_SHORT -> "\"year_short\""
                VariableLocalizationKey.ANNUAL -> "\"annual\""
                VariableLocalizationKey.ANNUALLY -> "\"annually\""
                VariableLocalizationKey.ANNUAL_SHORT -> "\"annual_short\""
                VariableLocalizationKey.FREE_PRICE -> "\"free_price\""
                VariableLocalizationKey.PERCENT -> "\"percent\""
                VariableLocalizationKey.NUM_DAY_ZERO -> "\"num_day_zero\""
                VariableLocalizationKey.NUM_DAY_ONE -> "\"num_day_one\""
                VariableLocalizationKey.NUM_DAY_TWO -> "\"num_day_two\""
                VariableLocalizationKey.NUM_DAY_FEW -> "\"num_day_few\""
                VariableLocalizationKey.NUM_DAY_MANY -> "\"num_day_many\""
                VariableLocalizationKey.NUM_DAY_OTHER -> "\"num_day_other\""
                VariableLocalizationKey.NUM_WEEK_ZERO -> "\"num_week_zero\""
                VariableLocalizationKey.NUM_WEEK_ONE -> "\"num_week_one\""
                VariableLocalizationKey.NUM_WEEK_TWO -> "\"num_week_two\""
                VariableLocalizationKey.NUM_WEEK_FEW -> "\"num_week_few\""
                VariableLocalizationKey.NUM_WEEK_MANY -> "\"num_week_many\""
                VariableLocalizationKey.NUM_WEEK_OTHER -> "\"num_week_other\""
                VariableLocalizationKey.NUM_MONTH_ZERO -> "\"num_month_zero\""
                VariableLocalizationKey.NUM_MONTH_ONE -> "\"num_month_one\""
                VariableLocalizationKey.NUM_MONTH_TWO -> "\"num_month_two\""
                VariableLocalizationKey.NUM_MONTH_FEW -> "\"num_month_few\""
                VariableLocalizationKey.NUM_MONTH_MANY -> "\"num_month_many\""
                VariableLocalizationKey.NUM_MONTH_OTHER -> "\"num_month_other\""
                VariableLocalizationKey.NUM_YEAR_ZERO -> "\"num_year_zero\""
                VariableLocalizationKey.NUM_YEAR_ONE -> "\"num_year_one\""
                VariableLocalizationKey.NUM_YEAR_TWO -> "\"num_year_two\""
                VariableLocalizationKey.NUM_YEAR_FEW -> "\"num_year_few\""
                VariableLocalizationKey.NUM_YEAR_MANY -> "\"num_year_many\""
                VariableLocalizationKey.NUM_YEAR_OTHER -> "\"num_year_other\""
            }
            arrayOf(serialized, expected)
        }
    }

    @Test
    fun `Should properly deserialize VariableLocalizationKey`() {
        // Arrange, Act
        val actual = OfferingParser.json.decodeFromString<VariableLocalizationKey>(serialized)

        // Assert
        assert(actual == expected)
    }

}
