package com.example.shiftalarmmvp.data

import java.time.LocalDate

enum class AlarmDateOverrideState {
    NONE,
    SKIP,
    ADD
}

data class AlarmDateOverrides private constructor(
    val skipDates: Set<LocalDate>,
    val addDates: Set<LocalDate>
) {
    fun stateFor(date: LocalDate): AlarmDateOverrideState {
        return when {
            date in skipDates -> AlarmDateOverrideState.SKIP
            date in addDates -> AlarmDateOverrideState.ADD
            else -> AlarmDateOverrideState.NONE
        }
    }

    fun withState(date: LocalDate, state: AlarmDateOverrideState): AlarmDateOverrides {
        return when (state) {
            AlarmDateOverrideState.NONE -> of(skipDates - date, addDates - date)
            AlarmDateOverrideState.SKIP -> of(skipDates + date, addDates - date)
            AlarmDateOverrideState.ADD -> of(skipDates - date, addDates + date)
        }
    }

    fun withoutSkip(date: LocalDate): AlarmDateOverrides = of(skipDates - date, addDates)

    fun withoutAdd(date: LocalDate): AlarmDateOverrides = of(skipDates, addDates - date)

    fun clearDates(dates: Set<LocalDate>): AlarmDateOverrides = of(skipDates - dates, addDates - dates)

    companion object {
        fun of(
            skipDates: Set<LocalDate> = emptySet(),
            addDates: Set<LocalDate> = emptySet()
        ): AlarmDateOverrides {
            val normalizedSkipDates = skipDates.toSet()
            val normalizedAddDates = addDates.toSet() - normalizedSkipDates
            return AlarmDateOverrides(
                skipDates = normalizedSkipDates,
                addDates = normalizedAddDates
            )
        }
    }
}

fun AlarmRule.normalizedDateOverrides(): AlarmDateOverrides {
    return AlarmDateOverrides.of(
        skipDates = skipDateEpochDays,
        addDates = addDateEpochDays
    )
}

fun AlarmRule.withDateOverrides(overrides: AlarmDateOverrides): AlarmRule {
    return copy(
        skipDateEpochDays = overrides.skipDates,
        addDateEpochDays = overrides.addDates
    )
}
