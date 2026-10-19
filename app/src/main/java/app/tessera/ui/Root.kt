package app.tessera.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.tessera.container
import app.tessera.data.Answer
import app.tessera.data.Person
import app.tessera.data.Question
import app.tessera.data.Settings
import app.tessera.domain.Due
import app.tessera.domain.ThisDay
import app.tessera.ru
import app.tessera.ui.answer.AnswerScreen
import app.tessera.ui.answer.DayScreen
import app.tessera.ui.components.rememberNow
import app.tessera.ui.home.HomeScreen
import app.tessera.ui.onboarding.WelcomeScreen
import app.tessera.ui.person.EditPersonScreen
import app.tessera.ui.person.PersonScreen
import app.tessera.ui.person.QuestionsScreen
import app.tessera.ui.settings.SettingsScreen
import app.tessera.ui.sitting.SittingScreen
import app.tessera.ui.theme.Smalt
import app.tessera.ui.theme.U
import app.tessera.ui.thread.ChorusScreen
import app.tessera.ui.thread.ThreadScreen
import java.time.LocalDate
import java.time.ZoneId

/** Everything stored, plus what is derived from it. Screens read through here. */
@Immutable
class Home(
    val people: List<Person>,
    val questions: List<Question>,
    val answers: List<Answer>,
    val settings: Settings,
    val ru: Boolean,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)

    private val questionById = questions.associateBy { it.id }
    private val answersByPerson = answers.groupBy { it.personId }

    fun person(id: Long) = people.firstOrNull { it.id == id }
    fun question(id: Long) = questionById[id]
    fun answer(id: Long) = answers.firstOrNull { it.id == id }

    fun questionsOf(personId: Long, archived: Boolean = false) = questions.filter { it.personId == personId && (archived || !it.archived) }
    fun answersOf(personId: Long): List<Answer> = answersByPerson[personId].orEmpty()
    fun answersTo(questionId: Long): List<Answer> = answers.filter { it.questionId == questionId }
    fun byDay(personId: Long): Map<Long, List<Answer>> = answersOf(personId).groupBy { it.day }

    fun glass(q: Question?): Color = Smalt.of(q?.glass ?: 0)
    fun glass(a: Answer): Color = glass(question(a.questionId))
    fun text(q: Question): String = q.text(ru)

    /** The question as it was asked that day; an older answer keeps its old wording. */
    fun asked(a: Answer): String = a.asked.ifEmpty { question(a.questionId)?.let(::text).orEmpty() }

    fun today(p: Person): List<Question> = Due.today(p, questions, answers, today, settings.perDay)
    fun interview(p: Person): List<Question> = Due.interview(p, questions, answers, today)
    val thisDay: List<Answer> by lazy { ThisDay.of(answers, today) }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun person(id: Long) = nav.navigate("person/$id") { launchSingleTop = true }
    fun editPerson(id: Long = 0) = nav.navigate("edit/$id") { launchSingleTop = true }
    fun questions(personId: Long) = nav.navigate("questions/$personId") { launchSingleTop = true }
    fun sitting(personId: Long, interview: Boolean = false) = nav.navigate("sitting/$personId/${if (interview) 1 else 0}") { launchSingleTop = true }
    fun thread(questionId: Long) = nav.navigate("thread/$questionId") { launchSingleTop = true }
    fun chorus(questionId: Long) = nav.navigate("chorus/$questionId") { launchSingleTop = true }
    fun day(personId: Long, day: LocalDate) = nav.navigate("day/$personId/${day.toEpochDay()}") { launchSingleTop = true }
    fun answer(id: Long) = nav.navigate("answer/$id") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun home() { nav.popBackStack("main", false) }
    fun back() { nav.popBackStack() }
}

@Composable
fun TesseraRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()

    val people by dao.observePeople().collectAsStateWithLifecycle(initialValue = null)
    val questions by dao.observeQuestions().collectAsStateWithLifecycle(initialValue = null)
    val answers by dao.observeAnswers().collectAsStateWithLifecycle(initialValue = null)
    // Past midnight the day rolls over: recompute what is due.
    val tick = rememberNow(60_000) / 3_600_000
    val ru = context.ru
    val home = remember(people, questions, answers, settings, tick, ru) {
        Home(people.orEmpty(), questions.orEmpty(), answers.orEmpty(), settings, ru)
    }
    val go = remember(nav) { Go(nav) }

    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome screens.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    val open by container.open.collectAsStateWithLifecycle()
    LaunchedEffect(open, people != null, settings.onboarded) {
        if (open == null || people == null || !settings.onboarded) return@LaunchedEffect
        container.open.value = null
        go.home()
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable("welcome") { WelcomeScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } } }
            composable("main") { HomeScreen(home, go) }
            composable("person/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PersonScreen(e.arguments?.getLong("id") ?: 0L, home, go)
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                EditPersonScreen(e.arguments?.getLong("id") ?: 0L, home, go)
            }
            composable("questions/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                QuestionsScreen(e.arguments?.getLong("id") ?: 0L, home, go)
            }
            composable(
                "sitting/{id}/{interview}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("interview") { type = NavType.IntType }),
            ) { e ->
                SittingScreen(e.arguments?.getLong("id") ?: 0L, (e.arguments?.getInt("interview") ?: 0) == 1, home, go)
            }
            composable("thread/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                ThreadScreen(e.arguments?.getLong("id") ?: 0L, home, go)
            }
            composable("chorus/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                ChorusScreen(e.arguments?.getLong("id") ?: 0L, home, go)
            }
            composable(
                "day/{id}/{day}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("day") { type = NavType.LongType }),
            ) { e ->
                DayScreen(e.arguments?.getLong("id") ?: 0L, LocalDate.ofEpochDay(e.arguments?.getLong("day") ?: 0L), home, go)
            }
            composable("answer/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                AnswerScreen(e.arguments?.getLong("id") ?: 0L, home, go)
            }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
