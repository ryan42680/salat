package app.prayertimes

import android.content.Context

enum class Prayer(val label: String) {
    FAJR("Fajr"), DHUHR("Dhuhr"), ASR("Asr"), MAGHRIB("Maghrib"), ISHA("Isha")
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun setLocation(c: Context, lat: Double, lon: Double) {
        sp(c).edit().putString("lat", lat.toString()).putString("lon", lon.toString()).apply()
    }

    fun location(c: Context): Pair<Double, Double>? {
        val lat = sp(c).getString("lat", null)?.toDoubleOrNull()
        val lon = sp(c).getString("lon", null)?.toDoubleOrNull()
        return if (lat != null && lon != null) lat to lon else null
    }

    fun method(c: Context): String = sp(c).getString("method", "MUSLIM_WORLD_LEAGUE")!!
    fun setMethod(c: Context, v: String) = sp(c).edit().putString("method", v).apply()

    fun hanafi(c: Context) = sp(c).getBoolean("hanafi", false)
    fun setHanafi(c: Context, v: Boolean) = sp(c).edit().putBoolean("hanafi", v).apply()

    fun enabled(c: Context, p: Prayer) = sp(c).getBoolean("on_" + p.name, true)
    fun setEnabled(c: Context, p: Prayer, v: Boolean) = sp(c).edit().putBoolean("on_" + p.name, v).apply()
}
