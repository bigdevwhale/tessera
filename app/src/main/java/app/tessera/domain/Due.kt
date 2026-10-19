package app.tessera.domain

import app.tessera.data.Answer
import app.tessera.data.Person
import app.tessera.data.Question
import java.time.LocalDate
import kotlin.math.abs

/**
 * What to ask whom today. Every question waits until its period (day, week, month, birthday year) has an
 * answer. Daily ones take turns so a long list never becomes a chore: at most [perDay] a day, the one
 * waiting longest first.
 */
object Due {
    fun answered(q: Question, answers: List<Answer>, today: LocalDate, birth: LocalDate?): Boolean {
        val from = Dates.periodStart(q.cadenceOf, today, birth).toEpochDay()
        return answers.any { it.questionId == q.id && it.day >= from && it.day <= today.toEpochDay() }
    }

    /** Today's short sitting: the daily turn plus any week or month still open. */
    fun today(person: Person, questions: List<Question>, answers: List<Answer>, today: LocalDate, perDay: Int): List<Question> {
        val mine = questions.filter { it.personId == person.id && !it.archived }
        val theirs = answers.filter { it.personId == person.id }
        val daily = mine.filter { it.cadenceOf == Cadence.DAILY }
        val doneToday = daily.count { answered(it, theirs, today, null) }
        val left = (perDay - doneToday).coerceAtLeast(0)
        val last = theirs.groupBy { it.questionId }.mapValues { (_, l) -> l.maxOf { it.day } }
        val turn = daily
            .filter { !answered(it, theirs, today, null) }
            .sortedWith(compareBy<Question>({ last[it.id] ?: Long.MIN_VALUE }, { shuffle(it.id, today) }))
            .take(left)
        val bigger = mine.filter { (it.cadenceOf == Cadence.WEEKLY || it.cadenceOf == Cadence.MONTHLY) && !answered(it, theirs, today, person.birthday) }
        return turn + bigger.sortedBy { it.cadenceOf.ordinal }
    }

    /** The birthday interview still open this year. */
    fun interview(person: Person, questions: List<Question>, answers: List<Answer>, today: LocalDate): List<Question> {
        val theirs = answers.filter { it.personId == person.id }
        return questions.filter { it.personId == person.id && !it.archived && it.cadenceOf == Cadence.YEARLY && !answered(it, theirs, today, person.birthday) }
    }

    /** Ties among daily questions shuffle differently each day, the same way all day. */
    private fun shuffle(id: Long, day: LocalDate): Long = (id * 2654435761L xor day.toEpochDay() * 40503L) and 0xffff

    /**
     * The answer to show after a new one, "a year ago you said…": the one nearest a year before [day], if one is
     * close enough, else the last one before this period. Null for a first answer.
     */
    fun echo(q: Question, answers: List<Answer>, day: LocalDate, birth: LocalDate?): Echo? {
        val from = Dates.periodStart(q.cadenceOf, day, birth).toEpochDay()
        val before = answers.filter { it.questionId == q.id && it.day < from }
        if (before.isEmpty()) return null
        val yearAgo = day.minusYears(1).toEpochDay()
        val near = before.minBy { abs(it.day - yearAgo) }
        val window = when (q.cadenceOf) {
            Cadence.DAILY -> 4
            Cadence.WEEKLY -> 10
            Cadence.MONTHLY -> 20
            Cadence.YEARLY -> 120
        }
        return if (abs(near.day - yearAgo) <= window) Echo(near, true) else Echo(before.maxBy { it.at }, false)
    }

    class Echo(val answer: Answer, val yearAgo: Boolean)
}
