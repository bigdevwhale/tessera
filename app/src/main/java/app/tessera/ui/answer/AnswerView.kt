package app.tessera.ui.answer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.tessera.data.Answer
import app.tessera.data.Media
import app.tessera.domain.Format
import app.tessera.ui.T
import app.tessera.ui.components.VideoPlayer
import app.tessera.ui.components.VoicePlayer
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType

/** Whether the media file of an answer is actually on this phone (a demo answer, or one restored without media, has none). */
fun Answer.playable(context: android.content.Context): Boolean = file.isNotEmpty() && Media.file(context, file).exists()

/**
 * An answer as it was given: a film (its frame until tapped), a voice note, or the written words. A film
 * also shows what was transcribed or typed under it, if anything.
 */
@Composable
fun AnswerBody(a: Answer, glass: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        when (a.kindOf) {
            Format.VIDEO -> FilmFrame(a, Modifier.fillMaxWidth().aspectRatio(3f / 4f))
            Format.VOICE -> if (a.playable(context)) VoicePlayer(a.file, a.durationMs, glass)
            Format.TEXT -> {}
        }
        if (a.text.isNotEmpty()) Text(a.text, style = UType.Said, modifier = Modifier.padding(top = if (a.kindOf == Format.TEXT) 0.dp else 12.dp))
    }
}

/** A film as its still frame with a play button; tapping plays it in place. */
@Composable
fun FilmFrame(a: Answer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var playing by remember(a.id) { mutableStateOf(false) }
    val frame = remember(a.thumb) { Media.bitmap(context, a.thumb, 720)?.asImageBitmap() }
    val canPlay = a.playable(context)
    Box(modifier.clip(U.RLg).background(Color.Black).clickable(enabled = canPlay) { playing = !playing }, contentAlignment = Alignment.Center) {
        if (playing) VideoPlayer(a.file, Modifier.fillMaxSize(), onEnd = { playing = false })
        else {
            if (frame != null) Image(frame, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (canPlay) Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(38.dp), tint = Color.White)
            }
            if (a.durationMs > 0) Text(
                T.clock(a.durationMs), style = UType.Small.copy(color = Color.White),
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
