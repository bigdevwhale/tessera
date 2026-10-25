package app.tessera.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.container
import app.tessera.data.Backup
import app.tessera.data.Everything
import app.tessera.notify.Alarms
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.Language
import app.tessera.ui.T
import app.tessera.ui.components.Chip
import app.tessera.ui.components.CommunityLinks
import app.tessera.ui.components.ConfirmDialog
import app.tessera.ui.components.InfoRow
import app.tessera.ui.components.SectionTitle
import app.tessera.ui.components.Segmented
import app.tessera.ui.components.Toggle
import app.tessera.ui.components.TopBar
import app.tessera.ui.components.UCard
import app.tessera.ui.ctx
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val s = home.settings
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf<android.net.Uri?>(null) }
    var picking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun set(change: (app.tessera.data.Settings) -> app.tessera.data.Settings) = container.write {
        settings.update(change)
        Alarms.reschedule(c)
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.write(c, uri, Everything(home.people, home.questions, home.answers)) }.isSuccess }
            busy = false
            Toast.makeText(c, if (ok) R.string.backup_done else R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoring = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            SectionTitle(stringResource(R.string.set_rhythm))
            UCard {
                Text(stringResource(R.string.set_per_day), style = UType.Strong)
                Text(stringResource(R.string.set_per_day_sub), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                Segmented(listOf(1, 2, 3, 5), s.perDay, { it.toString() }, { n -> set { it.copy(perDay = n) } })
            }

            SectionTitle(stringResource(R.string.set_reminder))
            UCard {
                Toggle(stringResource(R.string.set_remind), stringResource(R.string.set_remind_sub), s.remind) { v -> set { it.copy(remind = v) } }
                if (s.remind) InfoRow(Icons.Rounded.Schedule, stringResource(R.string.set_remind_at), T.time(s.remindAt)) { picking = true }
            }

            SectionTitle(stringResource(R.string.set_camera))
            UCard {
                Toggle(stringResource(R.string.set_ghost), stringResource(R.string.set_ghost_sub), s.ghost) { v -> set { it.copy(ghost = v) } }
                Toggle(stringResource(R.string.set_countdown), stringResource(R.string.set_countdown_sub), s.countdown) { v -> set { it.copy(countdown = v) } }
                Toggle(stringResource(R.string.set_echo), stringResource(R.string.set_echo_sub), s.echo) { v -> set { it.copy(echo = v) } }
            }

            SectionTitle(stringResource(R.string.set_language))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cur = Language.current()
                Chip(stringResource(R.string.set_lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
            }

            SectionTitle(stringResource(R.string.set_data))
            UCard {
                InfoRow(Icons.Rounded.Upload, stringResource(R.string.backup_export_sub), stringResource(if (busy) R.string.backup_busy else R.string.backup_export)) {
                    if (!busy) export.launch("tessera-${LocalDate.now()}.zip")
                }
                InfoRow(Icons.Rounded.Download, stringResource(R.string.backup_import_sub), stringResource(R.string.backup_import)) {
                    import.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
            }

            CommunityLinks()

            SectionTitle(stringResource(R.string.set_about))
            Text(stringResource(R.string.set_privacy), style = UType.Sub)
            Spacer(Modifier.height(28.dp))
        }
    }
    if (picking) RemindPicker(s.remindAt, { m -> set { it.copy(remindAt = m) }; picking = false }, { picking = false })
    restoring?.let { uri ->
        ConfirmDialog(stringResource(R.string.backup_import_title), stringResource(R.string.backup_import_text), stringResource(R.string.backup_import_go), {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val all = Backup.read(c, uri)
                        container.db.dao().replaceAll(all)
                        container.settings.update { it.copy(onboarded = true) }
                    }.isSuccess
                }
                Toast.makeText(c, if (ok) R.string.backup_restored else R.string.backup_bad, Toast.LENGTH_SHORT).show()
            }
        }, { restoring = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindPicker(minutes: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        text = { TimePicker(state, colors = TimePickerDefaults.colors(clockDialColor = U.c.surface, selectorColor = U.c.gold)) },
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok), color = U.c.gold, style = UType.Button) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}
