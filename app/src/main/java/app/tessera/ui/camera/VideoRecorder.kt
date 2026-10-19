package app.tessera.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.tessera.R
import app.tessera.data.Media
import app.tessera.ui.T
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.components.VideoPlayer
import app.tessera.ui.theme.Sans
import app.tessera.ui.theme.Serif
import app.tessera.ui.theme.U
import kotlinx.coroutines.delay

/** A finished take: the file in the media folder, and whether the selfie camera filmed it. */
class Take(val file: String, val front: Boolean)

private enum class Phase { READY, COUNTDOWN, RECORDING, REVIEW }

/**
 * Full-screen camera for one answer. The question stays at the top while you speak; last time's frame
 * ([ghost]) lies faintly over the picture so you can stand the same way and the years line up.
 * Recording stops by itself at [maxMs].
 */
@Composable
fun VideoRecorder(
    question: String,
    who: String,
    maxMs: Long,
    ghost: String?,
    countdown: Boolean,
    startFront: Boolean,
    onTake: (Take) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    fun has(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(has(Manifest.permission.CAMERA) && has(Manifest.permission.RECORD_AUDIO)) }
    var denied by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        granted = r.values.all { it } || (has(Manifest.permission.CAMERA) && has(Manifest.permission.RECORD_AUDIO))
        denied = !granted
    }
    val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    LaunchedEffect(Unit) { if (!granted) ask.launch(perms) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) CameraBody(question, who, maxMs, ghost, countdown, startFront, onTake, onClose)
        else Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.camera_needed), color = Color.White, fontFamily = Sans, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            if (denied) PrimaryButton(stringResource(R.string.try_again), { ask.launch(perms) })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.7f), fontFamily = Sans, modifier = Modifier.clickable(onClick = onClose).padding(12.dp))
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun CameraBody(
    question: String,
    who: String,
    maxMs: Long,
    ghost: String?,
    countdown: Boolean,
    startFront: Boolean,
    onTake: (Take) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            videoCaptureQualitySelector = QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))
            // Preview in the video's own shape, so the ghost frame and the picture crop the same way.
            previewResolutionSelector = ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY).build()
        }
    }
    DisposableEffect(owner) {
        controller.bindToLifecycle(owner)
        onDispose { controller.unbind() }
    }
    var front by remember { mutableStateOf(startFront) }
    LaunchedEffect(front) {
        val want = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        controller.cameraSelector = if (runCatching { controller.hasCamera(want) }.getOrDefault(true)) want else controller.cameraSelector
    }

    var phase by remember { mutableStateOf(Phase.READY) }
    var count by remember { mutableIntStateOf(3) }
    var started by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var take by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    val ghostBmp = remember(ghost) { ghost?.let { Media.bitmap(context, it, 720)?.asImageBitmap() } }

    fun discard() {
        take?.let { Media.delete(context, listOf(it)) }
        take = null
    }

    fun record() {
        val name = Media.newName("mp4")
        val options = FileOutputOptions.Builder(Media.file(context, name)).setDurationLimitMillis(maxMs).build()
        failed = false
        recording = runCatching {
            controller.startRecording(options, AudioConfig.create(true), ContextCompat.getMainExecutor(context)) { e ->
                when (e) {
                    is VideoRecordEvent.Start -> { started = System.currentTimeMillis(); phase = Phase.RECORDING }
                    is VideoRecordEvent.Finalize -> {
                        recording = null
                        val ok = !e.hasError() || e.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                        if (ok && Media.file(context, name).length() > 0) {
                            take = name
                            phase = Phase.REVIEW
                        } else {
                            Media.delete(context, listOf(name))
                            failed = true
                            phase = Phase.READY
                        }
                    }
                }
            }
        }.getOrElse { failed = true; phase = Phase.READY; null }
    }

    fun stop() = recording?.stop()

    fun go() {
        if (phase != Phase.READY) return
        if (countdown) {
            count = 3
            phase = Phase.COUNTDOWN
        } else record()
    }

    LaunchedEffect(phase) {
        if (phase == Phase.COUNTDOWN) {
            while (count > 0) {
                delay(800)
                count--
            }
            record()
        }
        while (phase == Phase.RECORDING) {
            now = System.currentTimeMillis()
            delay(100)
        }
    }
    DisposableEffect(Unit) { onDispose { recording?.stop() } }
    BackHandler {
        when (phase) {
            Phase.RECORDING -> stop()
            Phase.REVIEW -> { discard(); phase = Phase.READY }
            else -> onClose()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (phase == Phase.REVIEW && take != null) {
            VideoPlayer(take!!, Modifier.fillMaxSize(), loop = true)
        } else {
            AndroidView(
                factory = { PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } },
                modifier = Modifier.fillMaxSize(),
            )
            if (ghostBmp != null && (phase == Phase.READY || phase == Phase.COUNTDOWN)) Image(
                ghostBmp, null, contentScale = ContentScale.Crop,
                // Films are stored as the world sees them; the selfie preview is a mirror, so the ghost is turned to match it.
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f; scaleX = if (front) -1f else 1f },
            )
        }

        // The question, high up near the lens so the eyes stay on the camera.
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                .statusBarsPadding().padding(bottom = 36.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (phase == Phase.RECORDING) stop() else { discard(); onClose() } }) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White)
                }
                Text(who.uppercase(), color = Color.White.copy(alpha = 0.7f), fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (phase == Phase.READY) front = !front }) {
                    Icon(Icons.Rounded.Cameraswitch, stringResource(R.string.flip), tint = if (phase == Phase.READY) Color.White else Color.Transparent)
                }
            }
            Text(
                question, color = Color.White, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 33.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }

        if (phase == Phase.COUNTDOWN && count > 0) Text(
            count.toString(), color = Color.White, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 120.sp,
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .navigationBarsPadding().padding(top = 40.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (phase == Phase.REVIEW) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(U.RLg).border(1.dp, Color.White.copy(alpha = 0.5f), U.RLg)
                            .clickable { discard(); phase = Phase.READY },
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.retake), color = Color.White, fontFamily = Sans, fontWeight = FontWeight.Bold) }
                    PrimaryButton(stringResource(R.string.keep_take), { take?.let { val t = it; take = null; onTake(Take(t, front)) } }, Modifier.weight(1f))
                }
            } else {
                val left = if (phase == Phase.RECORDING) maxMs - (now - started) else maxMs
                Text(
                    if (phase == Phase.RECORDING) T.clock(now - started) else stringResource(R.string.up_to, T.clock(maxMs)),
                    color = Color.White, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (phase == Phase.RECORDING) U.c.danger else Color.White.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 4.dp),
                )
                if (failed) Text(stringResource(R.string.camera_failed), color = Color.White.copy(alpha = 0.85f), fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(16.dp))
                RecordButton(phase, (1f - left / maxMs.toFloat()).coerceIn(0f, 1f)) { if (phase == Phase.RECORDING) stop() else go() }
            }
        }
    }
}

/** The big round button; while recording, a gold ring fills as the time runs out and the dot turns into a stop square. */
@Composable
private fun RecordButton(phase: Phase, progress: Float, onClick: () -> Unit) {
    val gold = U.c.gold
    val red = U.c.danger
    Box(Modifier.size(86.dp).clip(CircleShape).clickable(enabled = phase == Phase.READY || phase == Phase.RECORDING, onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 5.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.85f), size.minDimension / 2 - w / 2, style = Stroke(w))
            if (phase == Phase.RECORDING) drawArc(gold, -90f, 360f * progress, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
        }
        if (phase == Phase.RECORDING) Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(red))
        else Box(Modifier.size(64.dp).clip(CircleShape).background(if (phase == Phase.READY) red else red.copy(alpha = 0.4f)))
    }
}
