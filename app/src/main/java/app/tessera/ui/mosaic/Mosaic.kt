package app.tessera.ui.mosaic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import app.tessera.data.Answer
import app.tessera.data.Media
import app.tessera.ui.theme.Sans
import app.tessera.ui.theme.Smalt
import app.tessera.ui.theme.U
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/** Bare plaster where nothing has been laid yet. */
private val Plaster = Color(0xFF1C2033)

/**
 * A stone cut by hand: the cell shrunk by the mortar line, each corner nudged a little by [seed], so no two
 * stones in a panel are quite the same and the rows waver like real opus tessellatum.
 */
fun tessera(r: Rect, seed: Long, jitter: Float = 0.11f): Path {
    val rnd = Random(seed)
    val jx = r.width * jitter
    val jy = r.height * jitter
    fun j(a: Float) = rnd.nextFloat() * a
    return Path().apply {
        moveTo(r.left + j(jx), r.top + j(jy))
        lineTo(r.right - j(jx), r.top + j(jy))
        lineTo(r.right - j(jx), r.bottom - j(jy))
        lineTo(r.left + j(jx), r.bottom - j(jy))
        close()
    }
}

/**
 * Lays one stone. Glass is lit from the upper left; a filmed answer shows its frame through a wash of its
 * glass; a starred one is gold leaf.
 */
fun DrawScope.smalt(r: Rect, seed: Long, glass: Color, image: ImageBitmap?, gold: Boolean) {
    val path = tessera(r, seed)
    clipPath(path) {
        when {
            gold -> drawRect(
                Brush.linearGradient(listOf(Color(0xFFF6D98A), Smalt.gold, Color(0xFF9C7424)), r.topLeft, r.bottomRight),
                r.topLeft, r.size,
            )
            image != null -> {
                // Centre-crop the frame into the stone.
                val k = maxOf(r.width / image.width, r.height / image.height)
                val w = image.width * k
                val h = image.height * k
                drawImage(
                    image,
                    dstOffset = IntOffset((r.center.x - w / 2).roundToInt(), (r.center.y - h / 2).roundToInt()),
                    dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                )
                drawRect(glass.copy(alpha = 0.26f), r.topLeft, r.size)
            }
            else -> drawRect(
                Brush.linearGradient(listOf(lerp(glass, Color.White, 0.18f), glass, lerp(glass, Color.Black, 0.32f)), r.topLeft, r.bottomRight),
                r.topLeft, r.size,
            )
        }
        // The glint on the glass.
        val g = Random(seed * 31).nextFloat()
        drawCircle(Color.White.copy(alpha = if (gold) 0.35f else 0.16f), r.minDimension * 0.12f, Offset(r.left + r.width * (0.22f + g * 0.2f), r.top + r.height * 0.24f))
    }
}

/** Splits a day's cell between its answers: one whole stone, two halves, or smaller pieces. */
fun pieces(r: Rect, n: Int): List<Rect> = when (n) {
    0, 1 -> listOf(r)
    2 -> listOf(Rect(r.left, r.top, r.center.x, r.bottom), Rect(r.center.x, r.top, r.right, r.bottom))
    3 -> listOf(Rect(r.left, r.top, r.center.x, r.bottom), Rect(r.center.x, r.top, r.right, r.center.y), Rect(r.center.x, r.center.y, r.right, r.bottom))
    else -> listOf(
        Rect(r.left, r.top, r.center.x, r.center.y), Rect(r.center.x, r.top, r.right, r.center.y),
        Rect(r.left, r.center.y, r.center.x, r.bottom), Rect(r.center.x, r.center.y, r.right, r.bottom),
    )
}

/** Frames of filmed answers, decoded small off the main thread. */
@Composable
fun rememberFrames(answers: List<Answer>, side: Int): Map<String, ImageBitmap> {
    val context = LocalContext.current
    val names = remember(answers) { answers.map { it.thumb }.filter { it.isNotEmpty() }.distinct() }
    val frames by produceState(emptyMap<String, ImageBitmap>(), names, side) {
        value = withContext(Dispatchers.IO) {
            names.mapNotNull { n -> Media.bitmap(context, n, side)?.let { n to it.asImageBitmap() } }.toMap()
        }
    }
    return frames
}

/**
 * A grid of days laid as mosaic. [cells] go row by row, [cols] to a row; null is outside the panel.
 * [labels] are written in the left margin at their row (month names). Tapping a laid or past day calls [onDay].
 */
@Composable
fun MosaicGrid(
    cells: List<LocalDate?>,
    cols: Int,
    byDay: Map<Long, List<Answer>>,
    glass: (Answer) -> Color,
    today: LocalDate,
    modifier: Modifier = Modifier,
    labels: List<Pair<Int, String>> = emptyList(),
    margin: Dp = 0.dp,
    onDay: ((LocalDate) -> Unit)? = null,
) {
    val laid = remember(byDay) { byDay.values.flatten() }
    val frames = rememberFrames(laid, 96)
    val density = LocalDensity.current
    val grout = U.c.grout
    val gold = U.c.gold
    val faint = U.c.line
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell = (maxWidth - margin) / cols
        val rows = (cells.size + cols - 1) / cols
        Box(Modifier.fillMaxWidth().height(cell * rows)) {
            labels.forEach { (row, text) ->
                Text(
                    text, fontFamily = Sans, fontSize = 10.sp, color = U.c.faint,
                    modifier = Modifier.offset(y = cell * row + cell / 2 - 7.dp),
                )
            }
            Canvas(
                Modifier.offset(x = margin).size(cell * cols, cell * rows)
                    .then(
                        if (onDay != null) Modifier.pointerInput(cells) {
                            detectTapGestures { p ->
                                val c = (p.x / with(density) { cell.toPx() }).toInt()
                                val r = (p.y / with(density) { cell.toPx() }).toInt()
                                cells.getOrNull(r * cols + c)?.takeIf { !it.isAfter(today) }?.let(onDay)
                            }
                        } else Modifier
                    ),
            ) {
                val s = cell.toPx()
                drawRect(grout)
                val gap = (s * 0.07f).coerceAtLeast(1f)
                cells.forEachIndexed { i, d ->
                    if (d == null) return@forEachIndexed
                    val rect = Rect(Offset((i % cols) * s + gap / 2, (i / cols) * s + gap / 2), Size(s - gap, s - gap))
                    val day = d.toEpochDay()
                    val list = byDay[day].orEmpty()
                    when {
                        list.isNotEmpty() -> {
                            val parts = pieces(rect, list.size.coerceAtMost(4))
                            list.take(4).forEachIndexed { k, a ->
                                val pr = parts[k].deflate(if (list.size > 1) gap / 3 else 0f)
                                smalt(pr, a.id * 7919 + day, glass(a), frames[a.thumb], a.starred)
                            }
                        }
                        d.isAfter(today) -> drawCircle(faint, s * 0.06f, rect.center)
                        else -> drawPath(tessera(rect, day), Plaster)
                    }
                    if (d == today) drawPath(tessera(rect.inflate(gap / 3), day), gold, style = Stroke(gap.coerceAtLeast(2f)))
                }
            }
        }
    }
}

/** One answer as a single big stone, for lists and the chorus. */
@Composable
fun Stone(answer: Answer, glass: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val frames = rememberFrames(listOf(answer), 160)
    Canvas(modifier.size(size)) {
        smalt(Rect(Offset.Zero, this.size), answer.id * 7919 + answer.day, glass, frames[answer.thumb], answer.starred)
    }
}

/** A stone with nothing in it yet: a question still waiting. */
@Composable
fun EmptyStone(glass: Color, modifier: Modifier = Modifier, size: Dp = 44.dp, seed: Long = 1) {
    Canvas(modifier.size(size)) {
        val r = Rect(Offset.Zero, this.size)
        drawPath(tessera(r, seed), glass.copy(alpha = 0.16f))
        drawPath(tessera(r, seed), glass.copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx()))
    }
}
