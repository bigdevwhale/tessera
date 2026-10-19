package app.tessera

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.tessera.data.DemoSeed
import app.tessera.domain.Cadence
import app.tessera.ru
import app.tessera.ui.Go
import app.tessera.ui.Home
import app.tessera.ui.answer.AnswerScreen
import app.tessera.ui.answer.DayScreen
import app.tessera.ui.home.HomeScreen
import app.tessera.ui.onboarding.WelcomeScreen
import app.tessera.ui.person.PersonScreen
import app.tessera.ui.person.QuestionsScreen
import app.tessera.ui.sitting.SittingScreen
import app.tessera.ui.theme.TesseraTheme
import app.tessera.ui.thread.ChorusScreen
import app.tessera.ui.thread.ThreadScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo family, drawn to PNGs (run with `-Prender`), English and Russian: a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TesseraApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val dao = c.db.dao()
        Home(dao.people(), dao.questions(), dao.answers(), c.settings.now(), app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            TesseraTheme { content(h, Go(nav)) }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private val Home.mila get() = people.first()
    private fun Home.yearly(key: String) = questions.first { it.personId == mila.id && it.key == key }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun main() = home().let { shot("1-home", it) { h, go -> HomeScreen(h, go) } }
    @Test fun person() = home().let { shot("2-panel", it) { h, go -> PersonScreen(h.mila.id, h, go) } }
    @Test fun sitting() = home().let { shot("3-sitting", it) { h, go -> SittingScreen(h.mila.id, false, h, go) } }
    @Test fun thread() = home().let { shot("4-through", it) { h, go -> ThreadScreen(h.yearly("y_be").id, h, go) } }
    @Test fun chorus() = home().let { shot("5-chorus", it) { h, go -> ChorusScreen(h.yearly("y_food").id, h, go) } }
    @Test fun daily() = home().let { h ->
        val q = h.questions.first { it.personId == h.mila.id && it.cadenceOf == Cadence.DAILY }
        shot("6-daily", h) { hh, go -> ThreadScreen(q.id, hh, go) }
    }
    @Test fun day() = home().let { h ->
        val a = h.answersOf(h.people[1].id).last { it.thumb.isNotEmpty() }
        shot("7-day", h) { hh, go -> DayScreen(a.personId, a.date, hh, go) }
    }
    @Test fun answer() = home().let { h ->
        val a = h.answersTo(h.yearly("y_be").id).last()
        shot("8-answer", h) { hh, go -> AnswerScreen(a.id, hh, go) }
    }
    @Test fun questions() = home().let { shot("9-questions", it) { h, go -> QuestionsScreen(h.mila.id, h, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
