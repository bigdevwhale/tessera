package app.tessera.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.tessera.R
import app.tessera.data.Media
import app.tessera.ui.T
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import kotlinx.coroutines.delay

private fun recorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()

/**
 * Records a voice answer of up to [maxMs]. [file] is the recorded file name (or null); [onChange] gets the new one
 * with its length, or null when it is removed.
 */
@Composable
fun VoiceRecorder(file: String?, lengthMs: Long, maxMs: Long, glass: Color, onChange: (String?, Long) -> Unit) {
    val context = LocalContext.current
    var rec by remember { mutableStateOf<MediaRecorder?>(null) }
    var recName by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    var level by remember { mutableFloatStateOf(0f) }

    fun stop() {
        val r = rec ?: return
        rec = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        val name = recName
        val length = (System.currentTimeMillis() - started).coerceAtMost(maxMs)
        if (ok && name != null && length > 600) {
            file?.let { Media.delete(context, listOf(it)) }
            onChange(name, length)
        } else name?.let { Media.delete(context, listOf(it)) }
    }

    fun start() {
        val name = Media.newName("m4a")
        val r = recorder(context)
        val ok = runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setMaxDuration(maxMs.toInt())
            r.setOutputFile(Media.file(context, name).absolutePath)
            r.prepare()
            r.start()
        }.isSuccess
        if (!ok) {
            r.release()
            return
        }
        r.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stop() }
        recName = name
        started = System.currentTimeMillis()
        rec = r
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) start() }
    DisposableEffect(Unit) { onDispose { stop() } }
    LaunchedEffect(rec) {
        while (rec != null) {
            now = System.currentTimeMillis()
            level = ((rec?.maxAmplitude ?: 0) / 12000f).coerceIn(0f, 1f) * 0.6f + level * 0.4f
            delay(120)
        }
    }

    val recording = rec != null
    when {
        recording -> Row(
            Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface2).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size((10 + 14 * level).dp).clip(CircleShape).background(U.c.danger))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.listening), style = UType.Strong, modifier = Modifier.weight(1f))
            Text(T.clock(now - started) + " / " + T.clock(maxMs), style = UType.Muted)
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = { stop() }) { Icon(Icons.Rounded.Stop, stringResource(R.string.stop), tint = U.c.gold) }
        }
        file != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            VoicePlayer(file, lengthMs, glass, Modifier.weight(1f))
            IconButton(onClick = { Media.delete(context, listOf(file)); onChange(null, 0) }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = U.c.faint)
            }
        }
        else -> PrimaryButton(
            stringResource(R.string.record_voice),
            {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
                else ask.launch(Manifest.permission.RECORD_AUDIO)
            },
            Modifier.fillMaxWidth(), icon = Icons.Rounded.Mic,
        )
    }
}

/** Plays a voice answer, with a progress line. [autoplay] starts it at once (the chorus). */
@Composable
fun VoicePlayer(file: String, lengthMs: Long, glass: Color, modifier: Modifier = Modifier, autoplay: Boolean = false, onEnd: () -> Unit = {}) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var position by remember { mutableLongStateOf(0L) }
    DisposableEffect(file) { onDispose { player?.release(); player = null } }
    LaunchedEffect(player) {
        while (player != null) {
            val p = player ?: break
            runCatching {
                position = p.currentPosition.toLong()
                progress = p.currentPosition / p.duration.coerceAtLeast(1).toFloat()
            }
            delay(100)
        }
    }
    fun toggle() {
        val p = player
        if (p != null) {
            p.release()
            player = null
            progress = 0f
            position = 0
            return
        }
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(Media.file(context, file).absolutePath)
                prepare()
                setOnCompletionListener { release(); player = null; progress = 0f; position = 0; onEnd() }
                start()
            }
        }.getOrElse { onEnd(); null }
    }
    LaunchedEffect(file, autoplay) { if (autoplay && player == null) toggle() }
    Row(
        modifier.clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { toggle() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(glass), contentAlignment = Alignment.Center) {
            Icon(if (player != null) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(U.c.line)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(glass))
        }
        Spacer(Modifier.width(12.dp))
        Text(T.clock(if (player != null) position else lengthMs), style = UType.Muted)
    }
}

/**
 * A filmed answer, played as stored: the way others see the person, not mirrored like the selfie preview.
 * [loop] for the review right after filming; [onEnd] for the chorus.
 */
@Composable
fun VideoPlayer(file: String, modifier: Modifier = Modifier, loop: Boolean = false, playing: Boolean = true, onEnd: () -> Unit = {}) {
    val context = LocalContext.current
    val path = remember(file) { Media.file(context, file).absolutePath }
    AndroidView(
        factory = { c ->
            VideoView(c).apply {
                setOnPreparedListener { mp -> mp.isLooping = loop; if (playing) start() }
                setOnCompletionListener { onEnd() }
                setOnErrorListener { _, _, _ -> onEnd(); true }
                setVideoPath(path)
            }
        },
        update = { v ->
            if (playing && !v.isPlaying) v.start() else if (!playing && v.isPlaying) v.pause()
        },
        onRelease = { it.stopPlayback() },
        modifier = modifier,
    )
}
