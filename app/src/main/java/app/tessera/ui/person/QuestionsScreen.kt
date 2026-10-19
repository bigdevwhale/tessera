package app.tessera.ui.person

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.container
import app.tessera.data.Family
import app.tessera.data.Media
import app.tessera.data.Person
import app.tessera.data.Question
import app.tessera.domain.Cadence
import app.tessera.domain.Format
import app.tessera.domain.Library
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.components.ConfirmDialog
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SectionTitle
import app.tessera.ui.components.Segmented
import app.tessera.ui.components.SoftButton
import app.tessera.ui.components.TopBar
import app.tessera.ui.components.UTextField
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.EmptyStone
import app.tessera.ui.theme.Smalt
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType

/** Which questions someone is asked, how often and how they usually answer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(personId: Long, home: Home, go: Go) {
    val c = ctx()
    val p = home.person(personId) ?: return
    var editing by remember { mutableStateOf<Question?>(null) }
    var adding by remember { mutableStateOf(false) }
    val all = home.questionsOf(personId, archived = true)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.questions_of, p.name), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            val daily = all.count { !it.archived && it.cadenceOf == Cadence.DAILY }
            if (daily > home.settings.perDay) Text(
                stringResource(R.string.turns_note, home.settings.perDay, daily), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Cadence.entries.forEach { cad ->
                val list = all.filter { it.cadenceOf == cad && !it.archived }
                if (list.isEmpty()) return@forEach
                SectionTitle(T.cadence(c, cad))
                list.forEach { q -> QuestionRow(q, home) { editing = q } }
            }
            val archived = all.filter { it.archived }
            if (archived.isNotEmpty()) {
                SectionTitle(stringResource(R.string.archived))
                archived.forEach { q -> QuestionRow(q, home) { editing = q } }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.add_question), { adding = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            Spacer(Modifier.height(28.dp))
        }
    }

    editing?.let { q ->
        ModalBottomSheet({ editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = U.c.surface) {
            EditQuestion(q, home) { editing = null }
        }
    }
    if (adding) ModalBottomSheet({ adding = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = U.c.surface) {
        AddQuestion(p, home) { adding = false }
    }
}

@Composable
private fun QuestionRow(q: Question, home: Home, onClick: () -> Unit) {
    val c = ctx()
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        EmptyStone(home.glass(q), size = 30.dp, seed = q.id)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(home.text(q), style = UType.Strong.copy(color = if (q.archived) U.c.muted else U.c.ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(T.format(c, q.formatOf), T.plural(c, R.plurals.answers, home.answersTo(q.id).size)).joinToString(" · "), style = UType.Small)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditQuestion(q: Question, home: Home, close: () -> Unit) {
    val c = ctx()
    val container = c.container
    var text by remember { mutableStateOf(q.text) }
    var cadence by remember { mutableStateOf(q.cadenceOf) }
    var format by remember { mutableStateOf(q.formatOf) }
    var glass by remember { mutableIntStateOf(q.glass) }
    var deleting by remember { mutableStateOf(false) }
    val count = home.answersTo(q.id).size
    val own = q.key.isEmpty()

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding()) {
        if (own) UTextField(text, { text = it }, minLines = 2) else Text(home.text(q), style = UType.Title)
        SectionTitle(stringResource(R.string.how_often))
        Segmented(Cadence.entries, cadence, { T.cadence(c, it) }, { cadence = it })
        SectionTitle(stringResource(R.string.usually_answer))
        Segmented(Format.entries, format, { T.format(c, it) }, { format = it })
        SectionTitle(stringResource(R.string.glass))
        GlassPicker(glass) { glass = it }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(stringResource(R.string.save), {
            container.write { db.dao().update(q.copy(text = if (own) text.trim() else q.text, cadence = cadence.name, format = format.name, glass = glass)) }
            close()
        }, Modifier.fillMaxWidth(), enabled = !own || text.isNotBlank())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(
                stringResource(if (q.archived) R.string.unarchive else R.string.archive),
                { container.write { db.dao().update(q.copy(archived = !q.archived)) }; close() },
                Modifier.weight(1f), icon = if (q.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
            )
            SoftButton(stringResource(R.string.delete), { deleting = true }, Modifier.weight(1f), color = U.c.danger, soft = U.c.surface2, icon = Icons.Rounded.DeleteOutline)
        }
        Text(stringResource(R.string.archive_note), style = UType.Small, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
    }
    if (deleting) ConfirmDialog(
        stringResource(R.string.delete_question_title),
        if (count > 0) T.plural(c, R.plurals.delete_question_text, count) else stringResource(R.string.delete_question_empty),
        stringResource(R.string.delete), {
            container.write { Media.delete(c, db.dao().deleteQuestion(q)) }
            close()
        }, { deleting = false }, danger = true,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassPicker(selected: Int, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Smalt.all.forEachIndexed { i, col ->
            Canvas(Modifier.size(34.dp).clip(CircleShape).clickable { onPick(i) }.border(if (i == selected) 3.dp else 0.dp, U.c.ink, CircleShape)) {
                drawCircle(col, size.minDimension / 2 - 4.dp.toPx())
            }
        }
    }
}

@Composable
private fun AddQuestion(p: Person, home: Home, close: () -> Unit) {
    val c = ctx()
    val container = c.container
    var own by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var cadence by remember { mutableStateOf(Cadence.DAILY) }
    var format by remember { mutableStateOf(p.bandOf.format(Cadence.DAILY)) }
    var glass by remember { mutableIntStateOf((home.questionsOf(p.id, true).size * 7) % Smalt.all.size) }
    val taken = home.questionsOf(p.id, archived = true).map { it.key }.toSet()

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding().navigationBarsPadding()) {
        Segmented(listOf(false, true), own, { stringResource(if (it) R.string.own_question else R.string.from_library) }, { own = it })
        Spacer(Modifier.height(12.dp))
        if (!own) {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                val list = Library.suggestions(p.bandOf, taken)
                Cadence.entries.forEach { cad ->
                    val part = list.filter { it.cadence == cad }
                    if (part.isEmpty()) return@forEach
                    SectionTitle(T.cadence(c, cad))
                    part.forEach { lq ->
                        val fits = p.bandOf in lq.bands
                        Row(
                            Modifier.fillMaxWidth().clickable { container.write { Family.addLibrary(db.dao(), p, lq) }; close() }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EmptyStone(Smalt.of(Family.glassOf(lq.key, 0)), size = 26.dp, seed = lq.key.hashCode().toLong())
                            Spacer(Modifier.width(12.dp))
                            Text(lq.text(home.ru), style = UType.Body.copy(color = if (fits) U.c.ink else U.c.muted), modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = U.c.gold)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        } else {
            UTextField(text, { text = it }, placeholder = stringResource(R.string.own_question_hint), minLines = 2)
            SectionTitle(stringResource(R.string.how_often))
            Segmented(Cadence.entries, cadence, { T.cadence(c, it) }, { cadence = it; format = p.bandOf.format(it) })
            SectionTitle(stringResource(R.string.usually_answer))
            Segmented(Format.entries, format, { T.format(c, it) }, { format = it })
            SectionTitle(stringResource(R.string.glass))
            GlassPicker(glass) { glass = it }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.add_question), {
                container.write { Family.addOwn(db.dao(), p, text, cadence, format, glass) }
                close()
            }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
            Spacer(Modifier.height(24.dp))
        }
    }
}
