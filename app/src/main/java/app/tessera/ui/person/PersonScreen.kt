package app.tessera.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.domain.Dates
import app.tessera.domain.Panel
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.components.Chip
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SectionTitle
import app.tessera.ui.components.SoftButton
import app.tessera.ui.components.TopBar
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.EmptyStone
import app.tessera.ui.mosaic.MosaicGrid
import app.tessera.ui.mosaic.Stone
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType

/** Someone's wall: a year's panel, and every question they are asked, to look at through the years. */
@Composable
fun PersonScreen(id: Long, home: Home, go: Go) {
    val c = ctx()
    val p = home.person(id) ?: return
    val answers = home.answersOf(id)
    val years = remember(answers, home.today) { Panel.years(answers, home.today) }
    var year by rememberSaveable { mutableIntStateOf(home.today.year) }
    val panel = remember(year) { Panel(year) }
    val cells = remember(panel) { (0 until panel.rows * Panel.COLS).map { panel.day(it % Panel.COLS, it / Panel.COLS) } }
    val byDay = remember(answers) { answers.groupBy { it.day } }
    val due = home.today(p)
    val interview = home.interview(p)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(p.name, go::back) {
            IconButton(onClick = { go.questions(id) }) { Icon(Icons.Rounded.Tune, stringResource(R.string.questions), tint = U.c.muted) }
            IconButton(onClick = { go.editPerson(id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            val age = Dates.age(p.birthday, home.today)
            Text(listOfNotNull(age?.let { T.age(c, it) }, T.band(c, p.bandOf)).joinToString(" · "), style = UType.Muted, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                years.forEach { y -> Chip(y.toString(), y == year, { year = y }) }
            }
            Spacer(Modifier.height(14.dp))
            MosaicGrid(
                cells, Panel.COLS, byDay, home::glass, home.today,
                labels = panel.monthRows.distinctBy { it.second }.map { (m, row) -> row to T.monthShort(c, m) },
                margin = 30.dp,
                onDay = { go.day(id, it) },
            )
            Spacer(Modifier.height(10.dp))
            val inYear = answers.filter { it.date.year == year }
            Text(
                stringResource(R.string.year_line, T.plural(c, R.plurals.stones, inYear.size), T.plural(c, R.plurals.days, inYear.map { it.day }.distinct().size)),
                style = UType.Small, modifier = Modifier.padding(start = 30.dp),
            )
            Spacer(Modifier.height(16.dp))
            if (interview.isNotEmpty()) {
                SoftButton(stringResource(R.string.interview_start, interview.size), { go.sitting(id, interview = true) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Cake)
                Spacer(Modifier.height(10.dp))
            }
            if (due.isNotEmpty()) PrimaryButton(stringResource(R.string.start_today, T.plural(c, R.plurals.questions, due.size)), { go.sitting(id) }, Modifier.fillMaxWidth())

            SectionTitle(stringResource(R.string.through_years))
            val qs = home.questionsOf(id, archived = true).filter { !it.archived || home.answersTo(it.id).isNotEmpty() }.sortedBy { it.cadenceOf.ordinal * 1000 + it.order }
            qs.forEach { q ->
                val list = home.answersTo(q.id)
                val last = list.maxByOrNull { it.at }
                Row(
                    Modifier.fillMaxWidth().clickable { go.thread(q.id) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (last != null) Stone(last, home.glass(q), size = 40.dp) else EmptyStone(home.glass(q), size = 40.dp, seed = q.id)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(home.text(q), style = UType.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val span = list.map { it.date.year }.distinct().size
                        val bits = listOfNotNull(
                            T.cadence(c, q.cadenceOf),
                            T.plural(c, R.plurals.answers, list.size),
                            if (span > 1) T.plural(c, R.plurals.years, span) else null,
                            if (q.archived) stringResource(R.string.archived) else null,
                        )
                        Text(bits.joinToString(" · "), style = UType.Small)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
