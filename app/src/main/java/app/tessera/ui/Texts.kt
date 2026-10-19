package app.tessera.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.tessera.R
import app.tessera.domain.Band
import app.tessera.domain.Cadence
import app.tessera.domain.Format
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Words and dates in the app's language. Plain functions over a [Context] so notifications can use them too. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "17 October 2026". */
    fun date(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale(c)))

    /** "17 October". */
    fun dayMonth(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM", locale(c)))

    /** "Saturday, 17 October". */
    fun weekday(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    /** "Oct", beside the panel. */
    fun monthShort(c: Context, month: Int): String =
        java.time.Month.of(month).getDisplayName(TextStyle.SHORT_STANDALONE, locale(c)).trimEnd('.').replaceFirstChar { it.titlecase(locale(c)) }

    fun cadence(c: Context, k: Cadence): String = c.getString(
        when (k) {
            Cadence.DAILY -> R.string.cad_daily
            Cadence.WEEKLY -> R.string.cad_weekly
            Cadence.MONTHLY -> R.string.cad_monthly
            Cadence.YEARLY -> R.string.cad_yearly
        }
    )

    fun format(c: Context, f: Format): String = c.getString(
        when (f) {
            Format.VIDEO -> R.string.fmt_video
            Format.VOICE -> R.string.fmt_voice
            Format.TEXT -> R.string.fmt_text
        }
    )

    fun band(c: Context, b: Band): String = c.getString(
        when (b) {
            Band.LITTLE -> R.string.band_little
            Band.KID -> R.string.band_kid
            Band.TEEN -> R.string.band_teen
            Band.ADULT -> R.string.band_adult
            Band.ELDER -> R.string.band_elder
        }
    )

    fun bandSub(c: Context, b: Band): String = c.getString(
        when (b) {
            Band.LITTLE -> R.string.band_little_sub
            Band.KID -> R.string.band_kid_sub
            Band.TEEN -> R.string.band_teen_sub
            Band.ADULT -> R.string.band_adult_sub
            Band.ELDER -> R.string.band_elder_sub
        }
    )

    /** "7 years old". */
    fun age(c: Context, years: Int): String = plural(c, R.plurals.age, years)

    /** "0:42" on the recording ring and players. */
    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    /** "19:30". */
    fun time(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
}

/** In-app language switching (persisted by AppCompat; shows up in Android 13+ per-app language settings). */
object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
