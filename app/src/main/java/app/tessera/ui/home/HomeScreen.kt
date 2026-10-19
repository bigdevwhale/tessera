package app.tessera.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tessera.R
import app.tessera.data.Person
import app.tessera.domain.Dates
import app.tessera.notify.Alarms
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SectionTitle
import app.tessera.ui.components.SoftButton
import app.tessera.ui.components.UCard
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.MosaicGrid
import app.tessera.ui.mosaic.Stone
import app.tessera.ui.theme.Smalt
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(home: Home, go: Go) {
    val c = ctx()
    // Ask once for the evening reminder, after the first person is set up.
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(home.people.isNotEmpty()) {
        if (home.people.isNotEmpty() && home.settings.remind && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(c)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(T.weekday(c, home.today).uppercase(), style = UType.Eyebrow)
                Text(stringResource(R.string.app_name), style = UType.Hero)
            }
            IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            if (home.thisDay.isNotEmpty()) ThisDayStrip(home, go)
            SectionTitle(stringResource(R.string.home_people))
            if (home.people.isEmpty()) Text(stringResource(R.string.home_empty), style = UType.Sub, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
            home.people.forEach { p ->
                PersonCard(p, home, go)
                Spacer(Modifier.height(12.dp))
            }
            SoftButton(stringResource(R.string.add_person), { go.editPerson() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** What was said on this date in earlier years: a row of stones with their year. */
@Composable
private fun ThisDayStrip(home: Home, go: Go) {
    val c = ctx()
    SectionTitle(stringResource(R.string.this_day, T.dayMonth(c, home.today)))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        home.thisDay.take(20).forEach { a ->
            val who = home.person(a.personId)?.name.orEmpty()
            Column(
                Modifier.width(150.dp).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { go.answer(a.id) }.padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stone(a, home.glass(a), size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(a.date.year.toString(), style = UType.Name)
                        Text(who, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(home.asked(a), style = UType.Small, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (a.text.isNotEmpty()) Text(a.text, style = UType.Said.copy(fontSize = 17.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PersonCard(p: Person, home: Home, go: Go) {
    val c = ctx()
    val due = home.today(p)
    val interview = home.interview(p)
    val age = Dates.age(p.birthday, home.today)
    val end = home.today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val cells: List<LocalDate?> = (69 downTo 0).map { end.minusDays(it.toLong()) }
    UCard(onClick = { go.person(p.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = UType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(age?.let { T.age(c, it) }, T.plural(c, R.plurals.stones, home.answersOf(p.id).size)).joinToString(" · ")
                Text(sub, style = UType.Muted)
            }
            Canvas(Modifier.width(14.dp).height(14.dp)) { drawCircle(Smalt.of(p.glass)) }
        }
        Spacer(Modifier.height(12.dp))
        MosaicGrid(cells, 14, home.byDay(p.id), home::glass, home.today, onDay = { go.day(p.id, it) })
        Spacer(Modifier.height(14.dp))
        if (interview.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(U.RMd).background(U.c.goldSoft).clickable { go.sitting(p.id, interview = true) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Cake, null, tint = U.c.gold)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.interview_waiting), style = UType.Strong.copy(color = U.c.gold))
                    Text(T.plural(c, R.plurals.questions, interview.size), style = UType.Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (due.isNotEmpty()) {
            PrimaryButton(stringResource(R.string.start_today, T.plural(c, R.plurals.questions, due.size)), { go.sitting(p.id) }, Modifier.fillMaxWidth())
        } else {
            Text(stringResource(R.string.all_laid), style = UType.Muted, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
