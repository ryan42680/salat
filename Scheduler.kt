package app.prayertimes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Keeps exactly ONE pending alarm: the next enabled prayer. When it fires, AlarmReceiver
 * schedules the following one before doing anything else, so the chain never depends on the app being alive.
 */
object Scheduler {
    private const val REQ_NEXT = 1001
    private const val REQ_TEST = 1002

    private fun pending(ctx: Context, req: Int, prayer: Prayer?, at: Long, test: Boolean): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra("at", at).putExtra("test", test)
        if (prayer != null) i.putExtra("prayer", prayer.name)
        return PendingIntent.getBroadcast(
            ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun set(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) // fallback: may be late
    }

    fun scheduleNext(ctx: Context, afterMs: Long = System.currentTimeMillis()) {
        val next = PrayerCalc.next(ctx, afterMs)
        if (next == null) {
            ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, REQ_NEXT, null, 0, false))
            return
        }
        set(ctx, next.second.time, pending(ctx, REQ_NEXT, next.first, next.second.time, false))
    }

    fun scheduleTest(ctx: Context) {
        set(ctx, System.currentTimeMillis() + 60_000, pending(ctx, REQ_TEST, null, 0, true))
    }
}
