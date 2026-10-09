package app.prayertimes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import java.util.Date

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.getBooleanExtra("test", false)) {
            Notifier.show(ctx, "Test alarm fired", "Scheduled alarms work on this device.", System.currentTimeMillis(), 99)
            return
        }
        val at = intent.getLongExtra("at", System.currentTimeMillis())
        // Schedule the next prayer FIRST so the chain survives anything that goes wrong below.
        Scheduler.scheduleNext(ctx, maxOf(System.currentTimeMillis(), at) + 1000)
        val prayer = intent.getStringExtra("prayer")
            ?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: return
        val time = DateFormat.getTimeFormat(ctx).format(Date(at))
        Notifier.show(ctx, "${prayer.label} time", "It's time for ${prayer.label} ($time)", at, prayer.ordinal)
    }
}

/** Boot, clock/timezone change, app update, exact-alarm permission change: rebuild the alarm. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.scheduleNext(ctx)
    }
}
