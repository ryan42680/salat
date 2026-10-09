package app.prayertimes

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Safety net: re-arms the alarm every 6 hours in case anything cleared it. */
class RescheduleWorker(ctx: Context, p: WorkerParameters) : Worker(ctx, p) {
    override fun doWork(): Result {
        Scheduler.scheduleNext(applicationContext)
        return Result.success()
    }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        Scheduler.scheduleNext(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reschedule", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RescheduleWorker>(6, TimeUnit.HOURS).build()
        )
    }
}
