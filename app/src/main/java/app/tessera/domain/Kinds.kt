package app.tessera.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** How often a question is asked. Each one keeps its own rhythm. */
enum class Cadence {
    DAILY, WEEKLY, MONTHLY, YEARLY;

    /** Longest answer the camera or microphone takes for this rhythm: a day is a glance, a year is a talk. */
    val maxMs: Long get() = when (this) {
        DAILY -> 15_000
        WEEKLY -> 60_000
        MONTHLY -> 120_000
        YEARLY -> 300_000
    }

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: DAILY
    }
}

/** How an answer is given. Every question has a usual way, but any answer may be given any way. */
enum class Format {
    VIDEO, VOICE, TEXT;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: TEXT
    }
}

/** Whose questions: the library picks a starting set by this. */
enum class Band {
    LITTLE, KID, TEEN, ADULT, ELDER;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: ADULT

        /** A sensible band for a birthday, when there is one. */
        fun forAge(years: Int?): Band = when {
            years == null -> ADULT
            years < 7 -> LITTLE
            years < 13 -> KID
            years < 18 -> TEEN
            years < 65 -> ADULT
            else -> ELDER
        }
    }

    /**
     * The usual way to answer a new question. Little ones can't type, so everything is filmed; for the
     * rest a day is a typed line, a week a voice note, and the bigger rhythms go on camera.
     */
    fun format(c: Cadence): Format = when {
        this == LITTLE -> Format.VIDEO
        c == Cadence.DAILY -> Format.TEXT
        c == Cadence.WEEKLY -> Format.VOICE
        else -> Format.VIDEO
    }
}

/** Calendar sums shared by the schedule, the panel and the screens. */
object Dates {
    /** The first day of the period [c] that [d] falls in. A year runs birthday to birthday, or from 1 January. */
    fun periodStart(c: Cadence, d: LocalDate, birth: LocalDate?): LocalDate = when (c) {
        Cadence.DAILY -> d
        Cadence.WEEKLY -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Cadence.MONTHLY -> d.withDayOfMonth(1)
        Cadence.YEARLY -> lastBirthday(d, birth)
    }

    /** The last birthday on or before [d] (29 February falls back to the 28th), or 1 January without one. */
    fun lastBirthday(d: LocalDate, birth: LocalDate?): LocalDate {
        if (birth == null) return d.withDayOfYear(1)
        val md = MonthDay.from(birth)
        val thisYear = md.atYear(d.year)
        return if (!thisYear.isAfter(d)) thisYear else md.atYear(d.year - 1)
    }

    fun isBirthday(d: LocalDate, birth: LocalDate?): Boolean = birth != null && lastBirthday(d, birth) == d && d != birth

    fun age(birth: LocalDate?, on: LocalDate): Int? = birth?.takeIf { !it.isAfter(on) }?.let { Period.between(it, on).years }

    fun parse(s: String?): LocalDate? = s?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun days(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)
}
