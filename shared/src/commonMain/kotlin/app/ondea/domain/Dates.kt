package app.ondea.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// Small helpers so the cycle logic reads the same on every platform.
fun LocalDate.plusDays(days: Long): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Long): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.isBefore(other: LocalDate): Boolean = this < other
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other
fun LocalDate.toEpochDay(): Long = toEpochDays().toLong()
fun epochDayToDate(day: Long): LocalDate = LocalDate.fromEpochDays(day)

/** Whole days from [from] to [to]; negative when [to] is earlier. */
fun daysBetween(from: LocalDate, to: LocalDate): Int = from.daysUntil(to)
