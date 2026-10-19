package app.tessera.ui.sitting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.tessera.R
import app.tessera.container
import app.tessera.data.Answer
import app.tessera.data.Media
import app.tessera.data.Question
import app.tessera.domain.Due
import app.tessera.domain.Format
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.answer.AnswerBody
import app.tessera.ui.answer.FilmFrame
import app.tessera.ui.camera.VideoRecorder
import app.tessera.ui.components.Chip
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.SoftButton
import app.tessera.ui.components.UCard
import app.tessera.ui.components.UTextField
import app.tessera.ui.components.VoiceRecorder
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.Stone
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the sitting is: before the first question (the interview only), answering, the echo of last year, or done. */
private sealed interface Step {
    data object Intro : Step
    data class Ask(val i: Int) : Step
    data class Echo(val i: Int, val echo: Due.Echo, val laid: Answer) : Step
    data object Done : Step
}

/**
 * Today's questions (or the birthday interview) one at a time. Each answer is laid as a stone; right after,
 * the earlier answer to the same question shows up, "a year ago you said…".
 */
@Composable
fun SittingScreen(personId: Long, interview: Boolean, home: Home, go: Go) {
    val p = home.person(personId) ?: return
    // Fixed when the sitting opens, so answering doesn't reshuffle the list underfoot.
    val list = remember { if (interview) home.interview(p) else home.today(p) }
    var step by remember { mutableStateOf<Step>(if (interview && list.isNotEmpty()) Step.Intro else if (list.isEmpty()) Step.Done else Step.Ask(0)) }
    val laid = remember { mutableStateListOf<Answer>() }

    fun next(i: Int) {
        step = if (i + 1 < list.size) Step.Ask(i + 1) else Step.Done
    }

    BackHandler { go.back() }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = go::back) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = U.c.muted) }
            Text(p.name, style = UType.Strong, modifier = Modifier.weight(1f))
            Progress(list.size, when (val s = step) {
                is Step.Ask -> s.i
                is Step.Echo -> s.i + 1
                Step.Done -> list.size
                Step.Intro -> 0
            })
            Spacer(Modifier.width(14.dp))
        }
        AnimatedContent(step, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) }, label = "step", modifier = Modifier.weight(1f)) { s ->
            when (s) {
                Step.Intro -> Intro(home, p.name, list.size) { step = Step.Ask(0) }
                is Step.Ask -> Ask(list[s.i], s.i, list.size, home, p.name, p.birthday) { a, echo ->
                    if (a != null) laid += a
                    if (a != null && echo != null && home.settings.echo) step = Step.Echo(s.i, echo, a) else next(s.i)
                }
                is Step.Echo -> EchoPage(s, home) { next(s.i) }
                Step.Done -> Done(laid, home, interview) { go.home(); go.person(personId) }
            }
        }
    }
}

/** A row of little stones, one per question, gold once answered. */
@Composable
private fun Progress(total: Int, done: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total.coerceAtMost(14)) { i ->
            Box(Modifier.width(10.dp).height(10.dp).background(if (i < done) U.c.gold else U.c.line))
        }
    }
}

@Composable
private fun Intro(home: Home, name: String, n: Int, onGo: () -> Unit) {
    val c = ctx()
    Column(Modifier.fillMaxSize().padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.interview_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.interview_title, name), style = UType.Display)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.interview_text, T.plural(c, R.plurals.questions, n)), style = UType.Sub)
        Spacer(Modifier.height(30.dp))
        PrimaryButton(stringResource(R.string.interview_go), onGo, Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Ask(q: Question, i: Int, total: Int, home: Home, who: String, birth: java.time.LocalDate?, onLaid: (Answer?, Due.Echo?) -> Unit) {
    val c = ctx()
    val container = c.container
    val scope = rememberCoroutineScope()
    val asked = home.text(q)
    var format by remember(q.id) { mutableStateOf(q.formatOf) }
    var text by remember(q.id) { mutableStateOf("") }
    var voice by remember(q.id) { mutableStateOf<String?>(null) }
    var voiceMs by remember(q.id) { mutableLongStateOf(0L) }
    var film by remember(q.id) { mutableStateOf<String?>(null) }
    // The film's frame and length, taken as soon as it is kept so the preview isn't black.
    var frame by remember(q.id) { mutableStateOf<Pair<String, Long>?>(null) }
    var front by remember(q.id) { mutableStateOf(true) }
    var camera by remember(q.id) { mutableStateOf(false) }
    var saving by remember(q.id) { mutableStateOf(false) }
    val past = home.answersTo(q.id)
    val ghost = past.filter { it.thumb.isNotEmpty() }.maxByOrNull { it.at }
    val ready = when (format) {
        Format.VIDEO -> film != null
        Format.VOICE -> voice != null
        Format.TEXT -> text.isNotBlank()
    }

    fun lay() {
        if (saving) return
        saving = true
        scope.launch {
            val today = home.today
            // The echo is looked up before the new stone exists, so it can only be an earlier one.
            val echo = Due.echo(q, past, today, birth)
            val a = withContext(Dispatchers.IO) {
                // Whatever was recorded in another format is let go.
                if (format != Format.VIDEO) film?.let { Media.delete(c, listOf(it)) }
                if (format != Format.VOICE) voice?.let { Media.delete(c, listOf(it)) }
                val base = Answer(personId = q.personId, questionId = q.id, day = today.toEpochDay(), kind = format.name, asked = asked, text = text.trim().takeIf { format == Format.TEXT }.orEmpty())
                val a = when (format) {
                    Format.VIDEO -> {
                        val f = film!!
                        val (thumb, ms) = frame ?: Media.frame(c, f) ?: ("" to Media.length(c, f))
                        base.copy(file = f, thumb = thumb, durationMs = ms, front = front)
                    }
                    Format.VOICE -> base.copy(file = voice!!, durationMs = voiceMs)
                    Format.TEXT -> base
                }
                a.copy(id = container.db.dao().insert(a))
            }
            onLaid(a, echo)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding()) {
        Spacer(Modifier.height(18.dp))
        Text(listOf(stringResource(R.string.question_n, i + 1, total), T.cadence(c, q.cadenceOf)).joinToString(" · ").uppercase(), style = UType.Eyebrow.copy(color = home.glass(q)))
        Spacer(Modifier.height(10.dp))
        Text(asked, style = UType.Ask)
        Spacer(Modifier.height(22.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Format.entries.forEach { f -> Chip(T.format(c, f), f == format, { format = f }) }
        }
        Spacer(Modifier.height(16.dp))
        when (format) {
            Format.VIDEO -> {
                val f = film
                if (f == null) PrimaryButton(stringResource(R.string.film_answer), { camera = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Videocam)
                else {
                    val fr = frame
                    FilmFrame(
                        Answer(personId = 0, questionId = 0, day = 0, kind = Format.VIDEO.name, file = f, thumb = fr?.first.orEmpty(), durationMs = fr?.second ?: 0, front = front),
                        Modifier.fillMaxWidth(0.62f).aspectRatio(9f / 16f).align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(10.dp))
                    SoftButton(stringResource(R.string.retake), {
                        Media.delete(c, listOfNotNull(f, fr?.first))
                        film = null; frame = null; camera = true
                    }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Videocam)
                }
            }
            Format.VOICE -> VoiceRecorder(voice, voiceMs, q.cadenceOf.maxMs, home.glass(q)) { f, ms -> voice = f; voiceMs = ms }
            Format.TEXT -> UTextField(text, { text = it }, placeholder = stringResource(R.string.answer_hint), minLines = 3)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.lay_stone), ::lay, Modifier.fillMaxWidth(), enabled = ready && !saving)
        Text(
            stringResource(R.string.skip), style = UType.Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally).clickable(enabled = !saving) {
                film?.let { Media.delete(c, listOf(it)) }
                voice?.let { Media.delete(c, listOf(it)) }
                onLaid(null, null)
            }.padding(16.dp),
        )
        if (saving) CircularProgressIndicator(color = U.c.gold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))
    }

    // Over everything, the sitting's own bar included: the camera wants the whole screen.
    if (camera) Dialog({ camera = false }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        VideoRecorder(
            question = asked, who = who, maxMs = q.cadenceOf.maxMs, ghost = if (home.settings.ghost) ghost?.thumb else null,
            countdown = home.settings.countdown, startFront = ghost?.front ?: true,
            onTake = { t ->
                film = t.file; front = t.front; camera = false
                scope.launch { frame = withContext(Dispatchers.IO) { Media.frame(c, t.file) } }
            },
            onClose = { camera = false },
        )
    }
}

/** Straight after laying a stone: what was said before. */
@Composable
private fun EchoPage(s: Step.Echo, home: Home, onNext: () -> Unit) {
    val c = ctx()
    val then = s.echo.answer
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
        Spacer(Modifier.height(18.dp))
        Text(
            (if (s.echo.yearAgo) stringResource(R.string.echo_year) else stringResource(R.string.echo_last)).uppercase(),
            style = UType.Eyebrow.copy(color = U.c.gold),
        )
        Spacer(Modifier.height(8.dp))
        Text(home.asked(then), style = UType.Title)
        Spacer(Modifier.height(4.dp))
        Text(T.date(c, then.date), style = UType.Muted)
        Spacer(Modifier.height(16.dp))
        UCard { AnswerBody(then, home.glass(then)) }
        if (s.laid.kindOf == Format.TEXT) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.echo_now).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(6.dp))
            Text(s.laid.text, style = UType.Said)
        }
        Spacer(Modifier.height(26.dp))
        PrimaryButton(stringResource(R.string.next), onNext, Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Done(laid: List<Answer>, home: Home, interview: Boolean, onWall: () -> Unit) {
    val c = ctx()
    Column(
        Modifier.fillMaxSize().padding(24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            laid.take(8).forEach { Stone(it, home.glass(it), size = 38.dp) }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            if (laid.isEmpty()) stringResource(R.string.done_none) else T.plural(c, R.plurals.done_laid, laid.size),
            style = UType.Display, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(if (interview) R.string.done_interview else R.string.done_text), style = UType.Sub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        PrimaryButton(stringResource(R.string.to_wall), onWall, Modifier.fillMaxWidth())
    }
}
