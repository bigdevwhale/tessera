package app.tessera.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** Daily questions asked per person per day; the rest take turns. */
    val perDay: Int = 2,
    val remind: Boolean = true,
    /** Minutes after midnight. */
    val remindAt: Int = 19 * 60 + 30,
    /** 3-2-1 before the camera starts. */
    val countdown: Boolean = true,
    /** Last time's face, faint over the camera, to stand the same way. */
    val ghost: Boolean = true,
    /** Show the earlier answer right after a new one. */
    val echo: Boolean = true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        perDay = this[PER_DAY] ?: 2,
        remind = this[REMIND] ?: true,
        remindAt = this[REMIND_AT] ?: (19 * 60 + 30),
        countdown = this[COUNTDOWN] ?: true,
        ghost = this[GHOST] ?: true,
        echo = this[ECHO] ?: true,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[PER_DAY] = s.perDay
            it[REMIND] = s.remind
            it[REMIND_AT] = s.remindAt
            it[COUNTDOWN] = s.countdown
            it[GHOST] = s.ghost
            it[ECHO] = s.echo
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PER_DAY = intPreferencesKey("per_day")
        val REMIND = booleanPreferencesKey("remind")
        val REMIND_AT = intPreferencesKey("remind_at")
        val COUNTDOWN = booleanPreferencesKey("countdown")
        val GHOST = booleanPreferencesKey("ghost")
        val ECHO = booleanPreferencesKey("echo")
    }
}
