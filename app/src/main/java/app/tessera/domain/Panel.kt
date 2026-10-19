package app.tessera.domain

import app.tessera.data.Answer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * A year laid as a mosaic panel: rows of two weeks, Monday first, one tile per day, the way a mosaicist
 * lays a course of stones and then the next.
 */
class Panel(val year: Int) {
    val first: LocalDate = LocalDate.of(year, 1, 1)
    val last: LocalDate = LocalDate.of(year, 12, 31)
    private val origin: LocalDate = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val rows: Int = (Dates.days(origin, last).toInt() / COLS) + 1

    /** Column and row of a day in this year. */
    fun cell(d: LocalDate): Pair<Int, Int> {
        val i = Dates.days(origin, d).toInt()
        return (i % COLS) to (i / COLS)
    }

    /** The day at a column and row, or null outside the year. */
    fun day(col: Int, row: Int): LocalDate? {
        if (col !in 0 until COLS || row !in 0 until rows) return null
        val d = origin.plusDays((row * COLS + col).toLong())
        return d.takeIf { it.year == year }
    }

    val days: List<LocalDate> get() = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()

    /** Row where each month begins, for the small month marks beside the panel. */
    val monthRows: List<Pair<Int, Int>> get() = (1..12).map { m -> m to cell(LocalDate.of(year, m, 1)).second }

    companion object {
        const val COLS = 14

        /** Years with at least one answer, plus this one, newest first. */
        fun years(answers: List<Answer>, today: LocalDate): List<Int> =
            (answers.map { it.date.year } + today.year).distinct().sortedDescending()
    }
}

/** The chorus of years: one answer per year to the same question, oldest first. */
object Chorus {
    /** A starred answer wins; then the one with the most to see or hear; then the latest. */
    fun pick(answers: List<Answer>): List<Answer> = answers
        .groupBy { it.date.year }
        .toSortedMap()
        .map { (_, l) ->
            l.maxWith(compareBy<Answer>({ it.starred }, { rank(it.kindOf) }, { it.durationMs + it.text.length * 50L }, { it.at }))
        }

    private fun rank(f: Format) = when (f) {
        Format.VIDEO -> 2
        Format.VOICE -> 1
        Format.TEXT -> 0
    }
}

/** "This day" on the home screen: what was said on today's date in earlier years. */
object ThisDay {
    fun of(answers: List<Answer>, today: LocalDate): List<Answer> = answers
        .filter { it.date.year < today.year && it.date.monthValue == today.monthValue && it.date.dayOfMonth == today.dayOfMonth }
        .sortedByDescending { it.day }
}
