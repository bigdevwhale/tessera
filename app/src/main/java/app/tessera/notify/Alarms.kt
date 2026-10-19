package app.tessera.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tessera.MainActivity
import app.tessera.R
import app.tessera.container
import app.tessera.domain.Dates
import app.tessera.domain.Due
import app.tessera.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One quiet reminder a day at the hour chosen, and only when someone still has a question waiting. */
object Alarms {
    private const val CHANNEL = "evening_v1"
    private const val ID = 7
    private const val ACTION = "app.tessera.notify.EVENING"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val l = context.localized()
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, l.getString(R.string.channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = l.getString(R.string.channel_desc)
                }
            )
        }
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The next moment at [minutes] after midnight, strictly after [now]. */
    fun next(minutes: Int, now: LocalDateTime, zone: ZoneId): Long {
        var at = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.atZone(zone).toInstant().toEpochMilli()
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        if (!s.remind || !s.onboarded) {
            am.cancel(p)
            return
        }
        val zone = ZoneId.systemDefault()
        // A reminder doesn't need to-the-second precision; this avoids the exact-alarm permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next(s.remindAt, LocalDateTime.now(zone), zone), p)
    }

    suspend fun fire(context: Context) {
        val s = context.container.settings.now()
        if (!s.remind || !s.onboarded || !canNotify(context)) return
        val dao = context.container.db.dao()
        val people = dao.people()
        val questions = dao.questions()
        val answers = dao.answers()
        val today = LocalDate.now()
        val waiting = people.filter {
            Due.today(it, questions, answers, today, s.perDay).isNotEmpty() || Due.interview(it, questions, answers, today).isNotEmpty()
        }
        if (waiting.isEmpty()) return
        val l = context.localized()
        val birthday = waiting.firstOrNull { Due.interview(it, questions, answers, today).isNotEmpty() && Dates.isBirthday(today, it.birthday) }
        val text = when {
            birthday != null -> l.getString(R.string.notif_birthday, birthday.name)
            waiting.size == 1 -> l.getString(R.string.notif_one, waiting[0].name)
            else -> l.getString(R.string.notif_many, waiting.joinToString(", ") { it.name })
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.gold))
            .setContentTitle(l.getString(R.string.notif_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 100,
                    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, "today")
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID, n)
        } catch (_: SecurityException) {
        }
    }

    fun clear(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    internal const val EVENING = ACTION
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Alarms.EVENING) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context)
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
