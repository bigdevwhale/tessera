package app.tessera

import app.tessera.data.Answer
import app.tessera.data.Person
import app.tessera.data.Question
import app.tessera.domain.Band
import app.tessera.domain.Cadence
import app.tessera.domain.Chorus
import app.tessera.domain.Dates
import app.tessera.domain.Due
import app.tessera.domain.Format
import app.tessera.domain.Library
import app.tessera.domain.Panel
import app.tessera.ui.thread.Drift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TesseraTest {
    private val mila = Person(id = 1, name = "Mila", birth = "2019-03-14", band = Band.KID.name)

    private fun q(id: Long, c: Cadence) = Question(id = id, personId = 1, key = "k$id", cadence = c.name)
    private fun a(id: Long, q: Long, d: LocalDate, text: String = "", starred: Boolean = false, kind: Format = Format.TEXT) =
        Answer(id = id, personId = 1, questionId = q, day = d.toEpochDay(), at = d.toEpochDay() * 86_400_000, text = text, starred = starred, kind = kind.name)

    @Test
    fun libraryKeysAreUniqueAndEveryBandHasAStart() {
        assertEquals(Library.all.size, Library.all.map { it.key }.toSet().size)
        Band.entries.forEach { b ->
            val start = Library.starter(b)
            assertTrue("$b has daily", start.any { it.cadence == Cadence.DAILY })
            assertTrue("$b has yearly", start.any { it.cadence == Cadence.YEARLY })
            assertTrue("$b starter fits", start.all { b in it.bands })
        }
    }

    @Test
    fun yearRunsBirthdayToBirthday() {
        val b = LocalDate.of(2019, 3, 14)
        assertEquals(LocalDate.of(2026, 3, 14), Dates.lastBirthday(LocalDate.of(2026, 10, 19), b))
        assertEquals(LocalDate.of(2025, 3, 14), Dates.lastBirthday(LocalDate.of(2026, 3, 13), b))
        assertEquals(LocalDate.of(2026, 1, 1), Dates.lastBirthday(LocalDate.of(2026, 10, 19), null))
        // A leap-day birthday falls back to 28 February.
        assertEquals(LocalDate.of(2025, 2, 28), Dates.lastBirthday(LocalDate.of(2025, 6, 1), LocalDate.of(2020, 2, 29)))
        assertTrue(Dates.isBirthday(LocalDate.of(2026, 3, 14), b))
        assertFalse(Dates.isBirthday(b, b))
    }

    @Test
    fun dailyQuestionsTakeTurns() {
        val today = LocalDate.of(2026, 10, 19)
        val qs = listOf(q(1, Cadence.DAILY), q(2, Cadence.DAILY), q(3, Cadence.DAILY))
        // 1 was answered yesterday, 2 a week ago, 3 never: 3 and 2 come first.
        val answers = listOf(a(1, 1, today.minusDays(1)), a(2, 2, today.minusDays(7)))
        assertEquals(listOf(3L, 2L), Due.today(mila, qs, answers, today, 2).map { it.id })
        // Once one is answered today, only one more is asked.
        val more = answers + a(3, 3, today)
        assertEquals(listOf(2L), Due.today(mila, qs, more, today, 2).map { it.id })
    }

    @Test
    fun weeksMonthsAndYearsWaitForTheirPeriod() {
        val today = LocalDate.of(2026, 10, 19) // a Monday
        val qs = listOf(q(1, Cadence.WEEKLY), q(2, Cadence.MONTHLY), q(3, Cadence.YEARLY))
        val answers = listOf(a(1, 1, today.minusDays(1)), a(2, 2, LocalDate.of(2026, 10, 2)), a(3, 3, LocalDate.of(2026, 3, 14)))
        // Last Sunday was the old week; this month and this birthday year are already answered.
        assertEquals(listOf(1L), Due.today(mila, qs, answers, today, 2).map { it.id })
        assertTrue(Due.interview(mila, qs, answers, today).isEmpty())
        assertEquals(listOf(3L), Due.interview(mila, qs, answers, LocalDate.of(2027, 3, 14)).map { it.id })
    }

    @Test
    fun echoFindsAYearAgoOrLastTime() {
        val today = LocalDate.of(2026, 10, 19)
        val daily = q(1, Cadence.DAILY)
        val yearAgo = a(1, 1, LocalDate.of(2025, 10, 20), "ducks")
        val recent = a(2, 1, LocalDate.of(2026, 10, 1), "snow")
        val e = Due.echo(daily, listOf(yearAgo, recent), today, null)
        assertNotNull(e)
        assertTrue(e!!.yearAgo)
        assertEquals(1L, e.answer.id)
        val last = Due.echo(daily, listOf(recent), today, null)!!
        assertFalse(last.yearAgo)
        assertNull(Due.echo(daily, emptyList(), today, null))
    }

    @Test
    fun panelHasEveryDayOnce() {
        listOf(2024, 2025, 2026).forEach { y ->
            val p = Panel(y)
            val cells = (0 until p.rows * Panel.COLS).mapNotNull { p.day(it % Panel.COLS, it / Panel.COLS) }
            assertEquals(p.days, cells)
            p.days.forEach { d -> val (c, r) = p.cell(d); assertEquals(d, p.day(c, r)) }
            assertTrue(p.rows <= 27)
        }
    }

    @Test
    fun chorusPicksOnePerYearGoldFirst() {
        val l = listOf(
            a(1, 1, LocalDate.of(2024, 3, 14), "dinosaur", kind = Format.VIDEO),
            a(2, 1, LocalDate.of(2025, 3, 14), "vet"),
            a(3, 1, LocalDate.of(2025, 6, 1), "astronaut", starred = true),
        )
        assertEquals(listOf(1L, 3L), Chorus.pick(l).map { it.id })
    }

    @Test
    fun heartwoodNeedsThreeSameYears() {
        val y = { n: Int, t: String -> a(n.toLong(), 1, LocalDate.of(n, 3, 14), t) }
        assertEquals(2024, Drift.heartwood(listOf(y(2023, "Pasta"), y(2024, "Pancakes"), y(2025, "pancakes."), y(2026, "Pancakes"))))
        assertNull(Drift.heartwood(listOf(y(2025, "Pasta"), y(2026, "Pasta"))))
        assertEquals(true, Drift.changed(y(2025, "Pink"), y(2026, "Turquoise")))
        assertEquals(false, Drift.changed(y(2025, "Pink"), y(2026, "pink!")))
    }
}
