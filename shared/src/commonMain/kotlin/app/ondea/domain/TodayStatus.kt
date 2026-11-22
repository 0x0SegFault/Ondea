package app.ondea.domain

import kotlinx.datetime.LocalDate

/** Colour family of the status, mapped to each platform's palette. */
enum class StatusTone { PERIOD, OVULATION, PEAK, FERTILE, NEUTRAL }

/** Headline of the Today screen. [key] is the text key on every platform (Android resource name). */
enum class Headline(val key: String, val plural: Boolean = false) {
    PERIOD_LATE("period_late", plural = true),
    OVULATION_CONFIRMED("ovulation_confirmed"),
    PEAK_FERTILITY("peak_fertility"),
    FERTILE_WINDOW("fertile_window"),
    FERTILE_IN("fertile_in", plural = true),
    WAITING_FOR_SHIFT("waiting_for_shift"),
}

/** Second line of the Today screen. Arguments: [TodayStatus.detailNumber] or [TodayStatus.detailDates]. */
enum class Detail(val key: String) {
    MAYBE_PREGNANT("maybe_pregnant"),
    LATE_HINT("late_hint"),
    OVULATED_ON("ovulated_on"),
    EGGWHITE_TODAY("eggwhite_today"),
    WAITING_MUCUS("waiting_mucus"),
    AFTER_LH("waiting_temperature_after_lh"),
    AFTER_MUCUS_PEAK("waiting_temperature_after_peak"),
    FERTILE_UNTIL("fertile_until_confirmed"),
    OVULATION_EXPECTED("ovulation_expected"),
    WAITING_FOR_SHIFT("waiting_for_shift_text"),
}

/** What the current ovulation date is based on. */
enum class Source(val key: String) {
    TEMPERATURE("source_temperature"),
    LH_TEST("source_lh"),
    MUCUS("source_mucus"),
    OWN_DAY("source_setting"),
    LEARNED("source_prediction"),
    TYPICAL("source_typical"),
}

data class TodayStatus(
    val tone: StatusTone,
    val headline: Headline,
    /** Count for plural headlines (days late, days until the fertile window). */
    val headlineCount: Int,
    val detail: Detail,
    /** Number argument of the detail (days of high temperature), if any. */
    val detailNumber: Int?,
    /** Date arguments of the detail, in order. */
    val detailDates: List<LocalDate>,
    val source: Source,
)

/** The Today message, decided once for Android and iOS. Null when tracking is paused. */
fun CycleEngine.todayStatus(): TodayStatus? {
    val f = forecast ?: return null
    val inFertile = !today.isBefore(f.fertileStart) && !today.isAfter(f.fertileEnd)
    val inPeak = !today.isBefore(f.peakStart) && !today.isAfter(f.ovulation)
    val pregnant = f.highDays >= 18
    val source = when (f.ovulationSource) {
        OvulationSource.TEMPERATURE -> Source.TEMPERATURE
        OvulationSource.LH_TEST -> Source.LH_TEST
        OvulationSource.MUCUS -> Source.MUCUS
        OvulationSource.PREDICTED -> when (predictionBasis) {
            PredictionBasis.OWN_DAY -> Source.OWN_DAY
            PredictionBasis.LEARNED -> Source.LEARNED
            PredictionBasis.TYPICAL -> Source.TYPICAL
        }
    }
    fun status(tone: StatusTone, headline: Headline, count: Int, detail: Detail, number: Int? = null, vararg dates: LocalDate) =
        TodayStatus(tone, headline, count, detail, number, dates.toList(), source)

    return when {
        f.daysLate > 0 ->
            if (pregnant) status(StatusTone.PERIOD, Headline.PERIOD_LATE, f.daysLate, Detail.MAYBE_PREGNANT, f.highDays)
            else status(StatusTone.PERIOD, Headline.PERIOD_LATE, f.daysLate, Detail.LATE_HINT)
        f.closed && today.isAfter(f.fertileEnd) ->
            if (pregnant) status(StatusTone.OVULATION, Headline.OVULATION_CONFIRMED, 0, Detail.MAYBE_PREGNANT, f.highDays)
            else status(StatusTone.OVULATION, Headline.OVULATION_CONFIRMED, 0, Detail.OVULATED_ON, null, f.ovulation, f.nextPeriod)
        inFertile -> {
            val peakToday = inPeak || f.mucusToday == 4
            val tone = if (peakToday) StatusTone.PEAK else StatusTone.FERTILE
            val headline = if (peakToday) Headline.PEAK_FERTILITY else Headline.FERTILE_WINDOW
            when {
                f.mucusToday == 4 -> status(tone, headline, 0, Detail.EGGWHITE_TODAY)
                f.window.waitingFor == Waiting.MUCUS -> status(tone, headline, 0, Detail.WAITING_MUCUS, null, f.fertileEnd)
                f.ovulationSource == OvulationSource.LH_TEST -> status(tone, headline, 0, Detail.AFTER_LH, null, f.ovulation)
                f.ovulationSource == OvulationSource.MUCUS -> status(tone, headline, 0, Detail.AFTER_MUCUS_PEAK, null, f.ovulation)
                f.ovulationSource == OvulationSource.TEMPERATURE -> status(tone, headline, 0, Detail.FERTILE_UNTIL, null, f.fertileEnd)
                else -> status(tone, headline, 0, Detail.OVULATION_EXPECTED, null, f.ovulation)
            }
        }
        today.isBefore(f.fertileStart) ->
            status(StatusTone.NEUTRAL, Headline.FERTILE_IN, daysBetween(today, f.fertileStart), Detail.OVULATION_EXPECTED, null, f.ovulation)
        else -> status(StatusTone.NEUTRAL, Headline.WAITING_FOR_SHIFT, 0, Detail.WAITING_FOR_SHIFT)
    }
}
