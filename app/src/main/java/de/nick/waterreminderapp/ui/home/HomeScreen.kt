package de.nick.waterreminderapp.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import de.nick.waterreminderapp.data.IntakeStore
import de.nick.waterreminderapp.data.Settings
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.scheduler.ReminderScheduler
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val snackbar      = remember { SnackbarHostState() }
    val intakeStore   = remember { IntakeStore(context) }
    val settingsStore = remember { SettingsStore(context) }

    val totalMl  by intakeStore.totalMlTodayFlow.collectAsState(initial = 0)
    val settings by settingsStore.settingsFlow.collectAsState(initial = Settings())
    val goalMl    = settings.goalMl

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderScheduler.start(context)
            scope.launch { snackbar.showSnackbar("Erinnerungen gestartet ✅") }
        } else {
            scope.launch { snackbar.showSnackbar("Benachrichtigungen nicht erlaubt ❌") }
        }
    }

    fun requestPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ReminderScheduler.start(context)
            scope.launch { snackbar.showSnackbar("Erinnerungen gestartet ✅") }
        }
    }

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
            Spacer(Modifier.height(16.dp))
            Text("💧 Water Reminder", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("$totalMl ml / $goalMl ml", style = MaterialTheme.typography.displaySmall)
            LinearProgressIndicator(
                progress = { (totalMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { intakeStore.addMl(250) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ 250 ml trinken")
            }
            Button(
                onClick = { requestPermissionAndStart() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶ Erinnerungen starten")
            }
            Button(
                onClick = {
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

