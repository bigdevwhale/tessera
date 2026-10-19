package app.tessera.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.data.Answer
import app.tessera.domain.Cadence
import app.tessera.domain.Chorus
import app.tessera.domain.Dates
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SectionTitle
import app.tessera.ui.components.TopBar
import app.tessera.ui.components.UCard
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.Stone
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType

/** What stays the same and what changes: the words of a question's answers through the years. */
object Drift {
    private fun norm(s: String) = s.lowercase().trim().trimEnd('.', '!', '?', '…').replace(Regex("\\s+"), " ")

    /** The year since which the answer has not changed, when it has held for at least three years. */
    fun heartwood(picks: List<Answer>): Int? {
        val withText = picks.filter { it.text.isNotBlank() }
        if (withText.size < 3) return null
        val last = norm(withText.last().text)
        val same = withText.reversed().takeWhile { norm(it.text) == last }
        return if (same.size >= 3) same.last().date.year else null
    }

    /** Was this year's answer different from the year before? Null when there's nothing to compare. */
    fun changed(prev: Answer?, now: Answer): Boolean? {
        if (prev == null || prev.text.isBlank() || now.text.isBlank()) return null
        return norm(prev.text) != norm(now.text)
    }

    /** Little words that say nothing about a year. */
    private val quiet = setOf(
        "with", "without", "that", "this", "there", "then", "when", "what", "very", "have", "from", "into", "about", "were", "they", "them",
        "только", "очень", "когда", "потом", "сегодня", "было", "были", "чтобы", "этот", "этой", "меня", "мной", "тоже", "всех", "есть",
    )

    /** The words said most often in a year of short answers, long words only. */
    fun words(answers: List<Answer>, n: Int = 6): List<String> = answers
        .flatMap { a -> a.text.lowercase().split(Regex("[^\\p{L}]+")).filter { it.length >= 4 && it !in quiet } }
        .groupingBy { it }.eachCount()
        .filter { it.value >= 2 }
        .entries.sortedByDescending { it.value }
        .take(n).map { it.key }
}

/** One question's answers, newest year first, with the chorus of years on top. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreadScreen(questionId: Long, home: Home, go: Go) {
    val c = ctx()
    val q = home.question(questionId) ?: return
    val p = home.person(q.personId)
    val answers = home.answersTo(questionId)
    val picks = remember(answers) { Chorus.pick(answers) }
    val byYear = remember(answers) { answers.groupBy { it.date.year }.toSortedMap(compareByDescending { it }) }
    val pickOf = picks.associateBy { it.date.year }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.through_years), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(listOfNotNull(p?.name, T.cadence(c, q.cadenceOf)).joinToString(" · ").uppercase(), style = UType.Eyebrow.copy(color = home.glass(q)), modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(6.dp))
            Text(home.text(q), style = UType.Display, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(16.dp))
            if (picks.isNotEmpty()) {
                PrimaryButton(stringResource(R.string.chorus_go, picks.size), { go.chorus(questionId) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.GraphicEq)
                Text(stringResource(R.string.chorus_sub), style = UType.Small, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            } else Text(stringResource(R.string.thread_empty), style = UType.Sub, modifier = Modifier.padding(4.dp))
            Drift.heartwood(picks)?.let { since ->
                Spacer(Modifier.height(14.dp))
                UCard {
                    Text(stringResource(R.string.heartwood, since).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
                    Spacer(Modifier.height(4.dp))
                    Text(picks.last().text, style = UType.Said)
                }
            }
            byYear.forEach { (year, list) ->
                val age = p?.birthday?.let { b -> Dates.age(b, list.maxBy { it.day }.date) }
                SectionTitle(listOfNotNull(year.toString(), age?.let { T.age(c, it) }, T.plural(c, R.plurals.answers, list.size)).joinToString(" · "))
                val many = list.size > 6 || q.cadenceOf == Cadence.DAILY
                if (many) {
                    val words = Drift.words(list)
                    if (words.isNotEmpty()) {
                        Text(stringResource(R.string.words_of_year), style = UType.Small)
                        Spacer(Modifier.height(4.dp))
                        Text(words.joinToString(" · "), style = UType.Said.copy(fontSize = UType.Said.fontSize * 0.9f, color = U.c.gold))
                        Spacer(Modifier.height(10.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        list.sortedBy { it.day }.forEach { a -> Stone(a, home.glass(a), Modifier.clip(U.RSm).clickable { go.answer(a.id) }, size = 22.dp) }
                    }
                    pickOf[year]?.let { best -> Spacer(Modifier.height(10.dp)); Quote(best, home, go) }
                } else {
                    list.sortedByDescending { it.day }.forEach { a ->
                        val prevYear = pickOf[year - 1]
                        val change = if (a == pickOf[year]) Drift.changed(prevYear, a) else null
                        Quote(a, home, go, change)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Quote(a: Answer, home: Home, go: Go, changed: Boolean? = null) {
    val c = ctx()
    Row(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).clickable { go.answer(a.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Stone(a, home.glass(a), size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(
                    T.dayMonth(c, a.date), T.format(c, a.kindOf),
                    when (changed) {
                        true -> stringResource(R.string.changed)
                        false -> stringResource(R.string.unchanged)
                        null -> null
                    },
                ).joinToString(" · "),
                style = UType.Small.copy(color = if (changed == true) U.c.gold else U.c.faint),
            )
            if (a.text.isNotEmpty()) Text(a.text, style = UType.Said.copy(fontSize = UType.Said.fontSize * 0.9f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            else if (a.durationMs > 0) Text(T.clock(a.durationMs), style = UType.Muted)
        }
    }
}
