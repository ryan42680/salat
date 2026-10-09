package app.prayertimes

import android.content.Context
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.Date

object PrayerCalc {
    val METHODS = listOf(
        CalculationMethod.MUSLIM_WORLD_LEAGUE,
        CalculationMethod.EGYPTIAN,
        CalculationMethod.KARACHI,
        CalculationMethod.UMM_AL_QURA,
        CalculationMethod.DUBAI,
        CalculationMethod.MOON_SIGHTING_COMMITTEE,
        CalculationMethod.NORTH_AMERICA,
        CalculationMethod.KUWAIT,
        CalculationMethod.QATAR,
        CalculationMethod.SINGAPORE,
    )

    private fun params(ctx: Context): CalculationParameters {
        val m = runCatching { CalculationMethod.valueOf(Prefs.method(ctx)) }
            .getOrDefault(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        val p = m.parameters
        p.madhab = if (Prefs.hanafi(ctx)) Madhab.HANAFI else Madhab.SHAFI
        return p
    }

    /** The five prayer times for the day containing [day], or null if no location is set. */
    fun times(ctx: Context, day: Date): Map<Prayer, Date>? {
        val (lat, lon) = Prefs.location(ctx) ?: return null
        val t = PrayerTimes(Coordinates(lat, lon), DateComponents.from(day), params(ctx))
        return mapOf(
            Prayer.FAJR to t.fajr,
            Prayer.DHUHR to t.dhuhr,
            Prayer.ASR to t.asr,
            Prayer.MAGHRIB to t.maghrib,
            Prayer.ISHA to t.isha,
        )
    }

    /** The next enabled prayer strictly after [afterMs], looking up to 3 days ahead. */
    fun next(ctx: Context, afterMs: Long): Pair<Prayer, Date>? {
        for (offset in 0..2) {
            val day = Date(afterMs + offset * 24L * 3600_000L)
            val map = times(ctx, day) ?: return null
            val hit = map.entries
                .filter { Prefs.enabled(ctx, it.key) && it.value.time > afterMs }
                .minByOrNull { it.value.time }
            if (hit != null) return hit.key to hit.value
        }
        return null
    }
}
