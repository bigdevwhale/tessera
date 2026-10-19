package app.tessera.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tessera.R
import app.tessera.container
import app.tessera.ui.components.PrimaryButton
import app.tessera.ui.ctx
import app.tessera.ui.mosaic.smalt
import app.tessera.ui.theme.Smalt
import app.tessera.ui.theme.U
import app.tessera.ui.theme.UType
import kotlin.random.Random

/** Three short truths about the app over a little panel of glass, then on to adding the first person. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val container = ctx().container
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Ornament(Modifier.fillMaxWidth().aspectRatio(2.6f))
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.welcome_title), style = UType.Hero)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.welcome_text), style = UType.Sub)
        Spacer(Modifier.height(22.dp))
        Point(0, stringResource(R.string.welcome_1))
        Point(3, stringResource(R.string.welcome_2))
        Point(7, stringResource(R.string.welcome_3))
        Spacer(Modifier.height(30.dp))
        PrimaryButton(stringResource(R.string.welcome_go), {
            container.write { settings.update { it.copy(onboarded = true) } }
            onDone()
        }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Point(glass: Int, text: String) {
    Row(Modifier.padding(vertical = 7.dp)) {
        Canvas(Modifier.padding(top = 3.dp).width(16.dp).height(16.dp)) { smalt(Rect(Offset.Zero, size), glass.toLong(), Smalt.of(glass), null, false) }
        Spacer(Modifier.width(14.dp))
        Text(text, style = UType.Body, modifier = Modifier.weight(1f))
    }
}

/** A strip of glass that turns gold in the middle: the days, and the one kept. */
@Composable
private fun Ornament(modifier: Modifier) {
    val grout = U.c.grout
    Canvas(modifier) {
        drawRect(grout)
        val cols = 14
        val s = size.width / cols
        val rows = (size.height / s).toInt()
        val rnd = Random(3)
        for (r in 0 until rows) for (c in 0 until cols) {
            val g = s * 0.07f
            val rect = Rect(Offset(c * s + g / 2, r * s + g / 2), Size(s - g, s - g))
            val gold = r == rows / 2 && c == cols / 2
            smalt(rect, (r * cols + c).toLong(), Smalt.of(rnd.nextInt(12)), null, gold)
        }
    }
}
