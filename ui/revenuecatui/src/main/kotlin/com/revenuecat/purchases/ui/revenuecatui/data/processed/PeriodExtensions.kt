package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import kotlin.math.roundToInt

internal val Period.periodUnitLocalizationKey: VariableLocalizationKey?
    get() = when (unit) {
        Period.Unit.DAY -> VariableLocalizationKey.DAY
        Period.Unit.WEEK -> VariableLocalizationKey.WEEK
        Period.Unit.MONTH -> VariableLocalizationKey.MONTH
        Period.Unit.YEAR -> VariableLocalizationKey.YEAR
        Period.Unit.UNKNOWN -> null
    }

internal val Period.periodUnitAbbreviatedLocalizationKey: VariableLocalizationKey?
    get() = when (unit) {
        Period.Unit.DAY -> VariableLocalizationKey.DAY_SHORT
        Period.Unit.WEEK -> VariableLocalizationKey.WEEK_SHORT
        Period.Unit.MONTH -> VariableLocalizationKey.MONTH_SHORT
        Period.Unit.YEAR -> VariableLocalizationKey.YEAR_SHORT
        Period.Unit.UNKNOWN -> null
    }

@Suppress("MagicNumber")
internal val Period.periodValueWithUnitLocalizationKey: VariableLocalizationKey
    get() = when {
        // Zero
        value == 0 && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_ZERO
        value == 0 && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_ZERO
        value == 0 && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_ZERO
        value == 0 && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_ZERO

        // One
        value == 1 && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_ONE
        value == 1 && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_ONE
        value == 1 && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_ONE
        value == 1 && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_ONE

        // Two
        value == 2 && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_TWO
        value == 2 && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_TWO
        value == 2 && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_TWO
        value == 2 && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_TWO

        // Few (3..4)
        (value in 3..4) && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_FEW
        (value in 3..4) && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_FEW
        (value in 3..4) && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_FEW
        (value in 3..4) && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_FEW

        // Many (5..10)
        (value in 5..10) && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_MANY
        (value in 5..10) && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_MANY
        (value in 5..10) && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_MANY
        (value in 5..10) && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_MANY

        // Other
        unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_OTHER
        unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_OTHER
        unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_OTHER
        unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_OTHER

        else -> VariableLocalizationKey.NUM_DAY_OTHER
    }

internal val Period.periodValueWithUnitAbbreviatedLocalizationKey: VariableLocalizationKey?
    get() = when (unit) {
        Period.Unit.DAY -> VariableLocalizationKey.NUM_DAYS_SHORT
        Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEKS_SHORT
        Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTHS_SHORT
        Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEARS_SHORT
        Period.Unit.UNKNOWN -> null
    }

internal val Period.roundedValueInDays: String
    get() = valueInDays.roundToInt().toString()

internal val Period.roundedValueInWeeks: String
    get() = valueInWeeks.roundToInt().toString()

internal val Period.roundedValueInMonths: String
    get() = valueInMonths.roundToInt().toString()

internal val Period.roundedValueInYears: String
    get() = valueInYears.roundToInt().toString()
