package app.tessera.ui.answer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.container
import app.tessera.data.Answer
import app.tessera.data.Media
import app.tessera.domain.Dates
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.components.ConfirmDialog
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SoftButton
import app.tessera.ui.components.TopBar
import app.tessera.ui.components.UCard
import app.tessera.ui.components.UTextField
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.Stone
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import java.time.LocalDate

/** Every stone laid on one day for one person. */
@Composable
fun DayScreen(personId: Long, day: LocalDate, home: Home, go: Go) {
    val c = ctx()
    val p = home.person(personId) ?: return
    val list = home.answersOf(personId).filter { it.day == day.toEpochDay() }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(T.weekday(c, day), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            val age = Dates.age(p.birthday, day)
            Text(listOfNotNull(p.name, age?.let { T.age(c, it) }, day.year.toString()).joinToString(" · "), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            if (list.isEmpty()) Text(stringResource(R.string.day_empty), style = UType.Sub, modifier = Modifier.padding(4.dp))
            list.forEach { a ->
                UCard(onClick = { go.answer(a.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stone(a, home.glass(a), size = 26.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(home.asked(a), style = UType.Strong, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    AnswerBody(a, home.glass(a))
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** One answer up close: play it, gild it, see the same question through the years, mend or remove it. */
@Composable
fun AnswerScreen(id: Long, home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val a = home.answer(id) ?: return
    val p = home.person(a.personId)
    var editing by remember { mutableStateOf(false) }
    var text by remember(a.id) { mutableStateOf(a.text) }
    var deleting by remember { mutableStateOf(false) }

    fun save(change: (Answer) -> Answer) = container.write { db.dao().update(change(a)) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(T.date(c, a.date), go::back) {
            IconButton(onClick = { save { it.copy(starred = !it.starred) } }) {
                Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.gild), tint = if (a.starred) U.c.gold else U.c.faint)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            val age = Dates.age(p?.birthday, a.date)
            Text(listOfNotNull(p?.name, age?.let { T.age(c, it) }).joinToString(" · ").uppercase(), style = UType.Eyebrow.copy(color = home.glass(a)), modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(6.dp))
            Text(home.asked(a), style = UType.Title, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(16.dp))
            if (editing) {
                UTextField(text, { text = it }, minLines = 3, placeholder = stringResource(R.string.note_hint))
                Spacer(Modifier.height(10.dp))
                PrimaryButton(stringResource(R.string.save), { save { it.copy(text = text.trim()) }; editing = false }, Modifier.fillMaxWidth())
            } else AnswerBody(a, home.glass(a))
            if (a.starred) Text(stringResource(R.string.gilded_note), style = UType.Small.copy(color = U.c.gold), modifier = Modifier.padding(top = 14.dp, start = 4.dp))
            Spacer(Modifier.height(24.dp))
            SoftButton(stringResource(R.string.through_years), { go.thread(a.questionId) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.History)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(if (a.text.isEmpty()) R.string.add_note else R.string.edit), { editing = true }, Modifier.weight(1f), icon = Icons.Rounded.Edit)
                SoftButton(stringResource(R.string.delete), { deleting = true }, Modifier.weight(1f), color = U.c.danger, soft = U.c.surface, icon = Icons.Rounded.DeleteOutline)
            }
            Text(
                stringResource(R.string.gild_hint), style = UType.Small,
                modifier = Modifier.padding(top = 14.dp, start = 4.dp).clickable { save { it.copy(starred = !it.starred) } },
            )
            Spacer(Modifier.height(28.dp))
        }
    }
    if (deleting) ConfirmDialog(stringResource(R.string.delete_answer_title), stringResource(R.string.delete_answer_text), stringResource(R.string.delete), {
        container.write {
            db.dao().delete(a)
            Media.delete(c, a.files)
        }
        go.back()
    }, { deleting = false }, danger = true)
}
