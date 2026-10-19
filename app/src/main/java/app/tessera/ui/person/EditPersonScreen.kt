package app.tessera.ui.person

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.container
import app.tessera.data.Family
import app.tessera.data.Media
import app.tessera.data.Person
import app.tessera.domain.Band
import app.tessera.domain.Dates
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.components.ConfirmDialog
import app.tessera.ui.components.InfoRow
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SectionTitle
import app.tessera.ui.components.SoftButton
import app.tessera.ui.components.TopBar
import app.tessera.ui.components.UCard
import app.tessera.ui.components.UTextField
import app.tessera.ui.ctx
import app.tessera.ui.theme.Smalt
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A new person, or someone's name, birthday and glass. A new person gets the library's starting questions for their age. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPersonScreen(id: Long, home: Home, go: Go) {
    val old = home.person(id)
    val c = ctx()
    val container = c.container
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    var birth by rememberSaveable { mutableStateOf(old?.birth.orEmpty()) }
    var band by rememberSaveable { mutableStateOf(old?.bandOf) }
    var glass by rememberSaveable { mutableIntStateOf(old?.glass ?: (home.people.size * 5 % Smalt.all.size)) }
    var picking by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val birthday = Dates.parse(birth)
    val shownBand = band ?: Band.forAge(Dates.age(birthday, home.today))

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.add_person else R.string.edit_person), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            SectionTitle(stringResource(R.string.name))
            UTextField(name, { name = it }, placeholder = stringResource(R.string.name_hint), words = true)

            SectionTitle(stringResource(R.string.birthday))
            UCard {
                InfoRow(Icons.Rounded.CalendarMonth, stringResource(R.string.birthday_sub), birthday?.let { T.date(c, it) } ?: stringResource(R.string.birthday_none)) { picking = true }
                if (birthday != null) Text(
                    stringResource(R.string.birthday_clear), style = UType.Small.copy(color = U.c.gold),
                    modifier = Modifier.clickable { birth = "" }.padding(top = 4.dp, bottom = 4.dp, start = 54.dp),
                )
            }

            SectionTitle(stringResource(R.string.band))
            Band.entries.forEach { b ->
                val on = b == shownBand
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(U.RMd)
                        .background(if (on) U.c.goldSoft else U.c.surface)
                        .border(1.dp, if (on) U.c.gold.copy(alpha = 0.5f) else U.c.line, U.RMd)
                        .clickable { band = b }.padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(T.band(c, b), style = UType.Strong.copy(color = if (on) U.c.gold else U.c.ink))
                        Text(T.bandSub(c, b), style = UType.Muted)
                    }
                }
            }
            if (old != null) Text(stringResource(R.string.band_note), style = UType.Small, modifier = Modifier.padding(top = 6.dp, start = 4.dp))

            SectionTitle(stringResource(R.string.glass))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Smalt.all.forEachIndexed { i, col ->
                    Canvas(
                        Modifier.size(38.dp).clip(CircleShape).clickable { glass = i }
                            .border(if (i == glass) 3.dp else 0.dp, U.c.ink, CircleShape),
                    ) { drawCircle(col, size.minDimension / 2 - 5.dp.toPx()) }
                }
            }

            Spacer(Modifier.height(26.dp))
            PrimaryButton(stringResource(if (old == null) R.string.add_person_go else R.string.save), {
                val clean = name.trim()
                scope.launch {
                    val dao = container.db.dao()
                    if (old == null) {
                        val newId = Family.add(dao, Person(name = clean, birth = birth, band = shownBand.name, glass = glass))
                        go.back()
                        go.person(newId)
                    } else {
                        dao.update(old.copy(name = clean, birth = birth, band = shownBand.name, glass = glass))
                        go.back()
                    }
                }
            }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
            if (old != null) {
                Spacer(Modifier.height(12.dp))
                SoftButton(stringResource(R.string.delete_person), { deleting = true }, Modifier.fillMaxWidth(), color = U.c.danger, soft = U.c.surface, icon = Icons.Rounded.DeleteOutline)
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (picking) BirthdayPicker(birthday, { birth = it?.toString().orEmpty(); picking = false }, { picking = false })
    if (deleting && old != null) ConfirmDialog(
        stringResource(R.string.delete_person_title, old.name), stringResource(R.string.delete_person_text), stringResource(R.string.delete), {
            container.write {
                val files = db.dao().deletePerson(old)
                Media.delete(c, files)
            }
            go.home()
        }, { deleting = false }, danger = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPicker(current: LocalDate?, onPick: (LocalDate?) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = 1900..LocalDate.now().year,
    )
    val colors = DatePickerDefaults.colors(containerColor = U.c.surface2)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton(onClick = { onPick(state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }) }) {
                Text(stringResource(R.string.ok), color = U.c.gold, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    ) { DatePicker(state, colors = colors) }
}
