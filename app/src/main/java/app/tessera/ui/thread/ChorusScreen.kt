package app.tessera.ui.thread

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tessera.BuildConfig
import app.tessera.R
import app.tessera.data.Answer
import app.tessera.data.Media
import app.tessera.domain.Chorus
import app.tessera.domain.Dates
import app.tessera.domain.Format
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.T
import app.tessera.ui.answer.playable
import app.tessera.ui.components.VideoPlayer
import app.tessera.ui.components.VoicePlayer
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.Stone
import app.tessera.ui.theme.Sans
import app.tessera.ui.theme.Serif
import app.tessera.ui.theme.U
import kotlinx.coroutines.delay

/**
 * The chorus of years: the same question answered year after year, played back to back. Films fill the
 * screen; a voice plays over its stone; written words stay up long enough to read.
 */
@Composable
fun ChorusScreen(questionId: Long, home: Home, go: Go) {
    val c = ctx()
    val q = home.question(questionId) ?: return
    val p = home.person(q.personId)
    val picks = remember(questionId) { Chorus.pick(home.answersTo(questionId)) }
    if (picks.isEmpty()) return
    var i by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(!BuildConfig.SHOTS) }

    fun next() {
        if (i + 1 < picks.size) i++ else playing = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(i, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) }, label = "year") { k ->
            Verse(picks[k], home, playing) { if (k == i) next() }
        }
        // The year, large, the way a mosaic dates itself in its border.
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                .statusBarsPadding().padding(bottom = 30.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = go::back) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White) }
                Text(home.text(q), color = Color.White.copy(alpha = 0.8f), fontFamily = Sans, fontSize = 14.sp, maxLines = 2, modifier = Modifier.weight(1f))
            }
            val a = picks[i]
            val age = Dates.age(p?.birthday, a.date)
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
                Text(a.date.year.toString(), color = U.c.gold, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 56.sp)
                if (age != null) Text("  " + T.age(c, age), color = Color.White, fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).navigationBarsPadding().padding(top = 30.dp, bottom = 16.dp)) {
            // One dot per year; the current one gold.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                picks.forEachIndexed { k, a ->
                    Column(Modifier.clickable { i = k }.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(if (k == i) 12.dp else 8.dp).clip(CircleShape).background(if (k == i) U.c.gold else Color.White.copy(alpha = 0.4f)))
                        Text("’" + (a.date.year % 100).toString().padStart(2, '0'), color = Color.White.copy(alpha = if (k == i) 1f else 0.5f), fontFamily = Sans, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (i > 0) i-- }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                IconButton(onClick = { if (!playing && i == picks.size - 1) i = 0; playing = !playing }) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(34.dp), tint = Color.White)
                }
                IconButton(onClick = { if (i + 1 < picks.size) i++ }) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.next), tint = Color.White) }
            }
        }
    }
}

/** One year's answer, filling the screen. Calls [onEnd] when it has been seen or heard. */
@Composable
private fun Verse(a: Answer, home: Home, playing: Boolean, onEnd: () -> Unit) {
    val context = LocalContext.current
    val canPlay = a.playable(context)
    val frame = remember(a.thumb) { Media.bitmap(context, a.thumb, 720)?.asImageBitmap() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            a.kindOf == Format.VIDEO && canPlay -> VideoPlayer(a.file, Modifier.fillMaxSize(), playing = playing, onEnd = onEnd)
            else -> {
                if (frame != null) Image(frame, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f })
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (frame == null) Stone(a, home.glass(a), size = 84.dp)
                    Spacer(Modifier.height(22.dp))
                    if (a.text.isNotEmpty()) Text(
                        "«" + a.text + "»", color = Color.White, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (a.kindOf == Format.VOICE && canPlay) {
                        Spacer(Modifier.height(20.dp))
                        VoicePlayer(a.file, a.durationMs, home.glass(a), autoplay = playing, onEnd = onEnd)
                    }
                }
                // Nothing to play: words stay up as long as it takes to read them.
                if (!(a.kindOf == Format.VOICE && canPlay)) LaunchedEffect(a.id, playing) {
                    if (!playing) return@LaunchedEffect
                    delay(2600L + a.text.length * 45L)
                    onEnd()
                }
            }
        }
    }
}
