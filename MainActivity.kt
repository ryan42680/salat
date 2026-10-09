package app.prayertimes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface { Screen(resumeTick.intValue) } } }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
        Scheduler.scheduleNext(this)
    }
}

@SuppressLint("MissingPermission")
private fun lastLocation(ctx: Context): Location? {
    val lm = ctx.getSystemService(LocationManager::class.java)
    return lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

@Composable
fun Screen(tick: Int) {
    val ctx = LocalContext.current
    var rev by remember { mutableIntStateOf(0) }
    val key = tick * 1000 + rev
    val changed = { rev++; Scheduler.scheduleNext(ctx) }

    val loc = remember(key) { Prefs.location(ctx) }
    var latText by remember { mutableStateOf(loc?.first?.toString() ?: "") }
    var lonText by remember { mutableStateOf(loc?.second?.toString() ?: "") }
    val times = remember(key) { PrayerCalc.times(ctx, Date()) }
    val next = remember(key) { PrayerCalc.next(ctx, System.currentTimeMillis()) }
    val tf = remember { android.text.format.DateFormat.getTimeFormat(ctx) }

    val fillFromGps: () -> Unit = {
        val l = lastLocation(ctx)
        if (l == null) toast(ctx, "No recent location available. Enter it manually.")
        else {
            latText = String.format(Locale.US, "%.4f", l.latitude)
            lonText = String.format(Locale.US, "%.4f", l.longitude)
        }
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) fillFromGps()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { rev++ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prayer Times", style = MaterialTheme.typography.headlineMedium)

        if (times == null) {
            Text("Set your location below to see today's times.")
        } else {
            next?.let { Text("Next alert: ${it.first.label} at ${tf.format(it.second)}") }
            Prayer.values().forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.label, fontWeight = if (next?.first == p) FontWeight.Bold else FontWeight.Normal)
                        Text(tf.format(times[p]!!))
                    }
                    Switch(checked = Prefs.enabled(ctx, p), onCheckedChange = { Prefs.setEnabled(ctx, p, it); changed() })
                }
            }
        }

        Text("Location", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = latText, onValueChange = { latText = it }, label = { Text("Latitude") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lonText, onValueChange = { lonText = it }, label = { Text("Longitude") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val la = latText.toDoubleOrNull()
                val lo = lonText.toDoubleOrNull()
                if (la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0) {
                    toast(ctx, "Enter valid coordinates")
                } else {
                    Prefs.setLocation(ctx, la, lo); changed()
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) fillFromGps() else locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }) { Text("Use my location") }
        }

        Text("Calculation", style = MaterialTheme.typography.titleMedium)
        var menu by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { menu = true }) {
                Text("Method: " + Prefs.method(ctx).lowercase().replace('_', ' '))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                PrayerCalc.METHODS.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.name.lowercase().replace('_', ' ')) },
                        onClick = { Prefs.setMethod(ctx, m.name); menu = false; changed() }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hanafi Asr", Modifier.weight(1f))
            Switch(checked = Prefs.hanafi(ctx), onCheckedChange = { Prefs.setHanafi(ctx, it); changed() })
        }

        Text("Reliability", style = MaterialTheme.typography.titleMedium)
        val notifOk = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        val exactOk = Scheduler.canExact(ctx)
        val battOk = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

        StatusRow("Notifications", notifOk) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
        StatusRow("Exact alarms", exactOk) {
            if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
            )
        }
        StatusRow("Battery unrestricted", battOk) {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        Text(
            "On some phones (Xiaomi, Huawei, Oppo, Samsung) also enable Autostart / disable \"sleeping apps\" for this app in system settings.",
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                Notifier.show(ctx, "Test notification", "Notifications are shown.", System.currentTimeMillis(), 98)
            }) { Text("Test now") }
            OutlinedButton(onClick = {
                Scheduler.scheduleTest(ctx); toast(ctx, "Test alarm in 1 minute. Lock the screen and wait.")
            }) { Text("Test alarm (1 min)") }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: " + if (ok) "OK" else "needs attention", Modifier.weight(1f))
        if (!ok) TextButton(onClick = onFix) { Text("Fix") }
    }
}
