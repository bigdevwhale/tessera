package app.tessera

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.tessera.data.DemoSeed
import app.tessera.data.Media
import app.tessera.data.SettingsStore
import app.tessera.data.TesseraDatabase
import app.tessera.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = TesseraDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where a tap from outside wants to go ("today") and when it was made. */
    val open = MutableStateFlow<Pair<String, Long>?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class TesseraApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@TesseraApp, container)
            Alarms.reschedule(this@TesseraApp)
            // A day after a recording was abandoned, its file is swept away.
            val keep = container.db.dao().answers().flatMap { it.files }.toSet()
            Media.sweep(this@TesseraApp, keep, System.currentTimeMillis() - 24 * 3600_000L)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as TesseraApp).container

/** Resources in the in-app language, for notifications, which AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? The library has both languages and picks by this. */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
