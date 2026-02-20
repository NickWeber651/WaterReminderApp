package de.nick.waterreminderapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.nick.waterreminderapp.ui.theme.WaterReminderAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaterReminderAppTheme {
                WaterReminderScreen()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// WaterReminderScreen – Haupt-UI
//
// Zeigt:
//   • Aktuell getrunkene ml heute  (totalMlToday aus IntakeStore)
//   • Tagesziel in ml              (goalMl aus SettingsStore)
//   • Fortschrittsbalken
//   • Button: Start Reminders
//   • Button: Stop Reminders
//   • Button: +250 ml hinzufügen
//
// collectAsState() abonniert den Flow – bei jeder Änderung wird die UI
// automatisch neu gezeichnet (Recomposition).
// ---------------------------------------------------------------------------
@Composable
fun WaterReminderScreen() {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val snackbar      = remember { SnackbarHostState() }

    // Stores – remember damit sie nicht bei jeder Recomposition neu erzeugt werden
    val intakeStore   = remember { IntakeStore(context) }
    val settingsStore = remember { SettingsStore(context) }

    // -----------------------------------------------------------------------
    // Flows als State abonnieren
    // collectAsState(initial = ...) liefert einen Startwert bis der erste
    // Flow-Wert ankommt – verhindert kurzes Flackern beim Start.
    // -----------------------------------------------------------------------
    val totalMl by intakeStore.totalMlTodayFlow.collectAsState(initial = 0)
    val settings by settingsStore.settingsFlow.collectAsState(initial = Settings())
    val goalMl = settings.goalMl

    // -----------------------------------------------------------------------
    // Permission-Launcher für POST_NOTIFICATIONS (Android 13+)
    // Wird aufgerufen wenn der Nutzer auf "Start" tippt und die Permission fehlt.
    // onResult: granted → Scheduler starten, denied → Snackbar zeigen
    // -----------------------------------------------------------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderScheduler.start(context)
            scope.launch { snackbar.showSnackbar("Erinnerungen gestartet ✅") }
        } else {
            scope.launch { snackbar.showSnackbar("Benachrichtigungen nicht erlaubt ❌") }
        }
    }

    // -----------------------------------------------------------------------
    // Hilfsfunktion: Permission prüfen und ggf. anfordern, dann starten
    // -----------------------------------------------------------------------
    fun requestPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Unter Android 13 keine Laufzeit-Permission nötig
            ReminderScheduler.start(context)
            scope.launch { snackbar.showSnackbar("Erinnerungen gestartet ✅") }
        }
    }

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Titel
            Text(
                text  = "💧 Water Reminder",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Anzeige: ml heute / Ziel
            Text(
                text  = "$totalMl ml / $goalMl ml",
                style = MaterialTheme.typography.displaySmall
            )

            // Fortschrittsbalken
            // coerceIn(0f, 1f) verhindert Werte > 1 wenn Ziel überschritten
            LinearProgressIndicator(
                progress  = { (totalMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) },
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Button: +250 ml
            Button(
                onClick  = { scope.launch { intakeStore.addMl(250) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ 250 ml trinken")
            }

            // Button: Erinnerungen starten
            Button(
                onClick  = { requestPermissionAndStart() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶ Erinnerungen starten")
            }

            // Button: Erinnerungen stoppen
            Button(
                onClick  = {
                    ReminderScheduler.stop(context)
                    scope.launch { snackbar.showSnackbar("Erinnerungen gestoppt ⏹") }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⏹ Erinnerungen stoppen")
            }
        }
    }
}