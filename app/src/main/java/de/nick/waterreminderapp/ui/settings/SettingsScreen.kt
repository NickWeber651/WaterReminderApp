package de.nick.waterreminderapp.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.scheduler.ContextReminderScheduler
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Voreingestellte Tagesziel-Optionen in ml
private val GOAL_PRESETS = listOf(1500, 2000, 2500, 3000)

// Stunden für Start/Ende-Auswahl
private val HOUR_OPTIONS_START = listOf(5, 6, 7, 8, 9, 10)
private val HOUR_OPTIONS_END   = listOf(20, 21, 22, 23)

// Intervall-Voreinstellungen in Minuten
private val INTERVAL_PRESETS = listOf(15, 30, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            store     = SettingsStore(LocalContext.current),
            scheduler = ContextReminderScheduler(LocalContext.current)
        )
    )
) {
    val uiState  by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val v        = uiState.validationResult
    val scope    = rememberCoroutineScope()

    // Permission-Launcher für POST_NOTIFICATIONS (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.onRemindersEnabledChange(true)
            scope.launch { snackbar.showSnackbar("Erinnerungen gestartet ✅") }
        } else {
            scope.launch { snackbar.showSnackbar("Benachrichtigungen nicht erlaubt ❌") }
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                SettingsEvent.SavedSuccess -> snackbar.showSnackbar("Gespeichert ✅")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── 1. Tagesziel ──────────────────────────────────────────────
            SettingsCard(
                icon  = Icons.Filled.Opacity,
                title = "Tagesziel"
            ) {
                val goalValue = uiState.goalMlInput.trim().toIntOrNull() ?: 2000

                // Slider
                Text(
                    text  = "${goalValue} ml",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value         = goalValue.toFloat().coerceIn(500f, 4000f),
                    onValueChange = { vm.onGoalMlChange(it.roundToInt().toString()) },
                    valueRange    = 500f..4000f,
                    steps         = 69, // (4000-500)/50 - 1 = 69 Schritte à 50ml
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("500 ml", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("4.000 ml", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Schnellauswahl-Chips
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = goalValue == preset,
                            onClick  = { vm.onGoalMlChange(preset.toString()) },
                            label    = { Text("$preset ml") }
                        )
                    }
                }

                // Fehler-Text
                if (v.goalMlError != null) {
                    Text(
                        text  = v.goalMlError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── 2. Erinnerungsintervall ───────────────────────────────────
            SettingsCard(
                icon  = Icons.Filled.Notifications,
                title = "Erinnerungsintervall"
            ) {
                val intervalValue = uiState.intervalMinutesInput.trim().toIntOrNull() ?: 60

                Text(
                    text  = formatInterval(intervalValue),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))

                // Intervall-Chips
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    INTERVAL_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = intervalValue == preset,
                            onClick  = { vm.onIntervalMinutesChange(preset.toString()) },
                            label    = { Text(formatInterval(preset)) }
                        )
                    }
                }

                // Freie Eingabe für andere Werte
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value           = uiState.intervalMinutesInput,
                    onValueChange   = vm::onIntervalMinutesChange,
                    label           = { Text("Anderer Wert (Minuten)") },
                    isError         = v.intervalMinutesError != null,
                    supportingText  = v.intervalMinutesError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth()
                )

                if (v.intervalMinutesError == null && intervalValue < 15) {
                    Text(
                        text  = "⚡ Unter 15 Min. läuft der Reminder im OneTime-Modus (mehr Akkuverbrauch).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── 3. Zeitfenster ────────────────────────────────────────────
            SettingsCard(
                icon  = Icons.Filled.AccessTime,
                title = "Zeitfenster"
            ) {
                val weekdayStart = uiState.weekdayStartHourInput.trim().toIntOrNull() ?: 8
                val weekendStart = uiState.weekendStartHourInput.trim().toIntOrNull() ?: 9
                val endHour      = uiState.endHourInput.trim().toIntOrNull() ?: 22

                // Wochentag Start
                Text(
                    text  = "Wochentag – Start",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HOUR_OPTIONS_START.forEach { h ->
                        FilterChip(
                            selected = weekdayStart == h,
                            onClick  = { vm.onWeekdayStartHourChange(h.toString()) },
                            label    = { Text("${h}:00") }
                        )
                    }
                }
                if (v.weekdayStartHourError != null) {
                    Text(v.weekdayStartHourError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))

                // Wochenende Start
                Text(
                    text  = "Wochenende – Start",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HOUR_OPTIONS_START.forEach { h ->
                        FilterChip(
                            selected = weekendStart == h,
                            onClick  = { vm.onWeekendStartHourChange(h.toString()) },
                            label    = { Text("${h}:00") }
                        )
                    }
                }
                if (v.weekendStartHourError != null) {
                    Text(v.weekendStartHourError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))

                // Endzeit
                Text(
                    text  = "Ende",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HOUR_OPTIONS_END.forEach { h ->
                        FilterChip(
                            selected = endHour == h,
                            onClick  = { vm.onEndHourChange(h.toString()) },
                            label    = { Text("${h}:00") }
                        )
                    }
                }
                if (v.endHourError != null) {
                    Text(v.endHourError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            // ── 4. Erinnerungen an/aus ────────────────────────────────────
            SettingsCard(
                icon  = Icons.Filled.Notifications,
                title = "Erinnerungen"
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = if (uiState.remindersEnabled) "Erinnerungen aktiv" else "Erinnerungen deaktiviert",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked         = uiState.remindersEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Ab Android 13: Erst Permission anfragen
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    vm.onRemindersEnabledChange(true)
                                    scope.launch { snackbar.showSnackbar("Erinnerungen gestartet ✅") }
                                }
                            } else {
                                vm.onRemindersEnabledChange(false)
                                scope.launch { snackbar.showSnackbar("Erinnerungen gestoppt") }
                            }
                        }
                    )
                }
            }

            // ── Speichern ─────────────────────────────────────────────────
            Button(
                onClick  = vm::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Wiederverwendbare Section-Card ────────────────────────────────────────────

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card-Header mit Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

// ── Hilfsfunktion: Minuten leserlich formatieren ──────────────────────────────

private fun formatInterval(minutes: Int): String = when {
    minutes < 60  -> "$minutes Min."
    minutes == 60 -> "1 Std."
    minutes % 60 == 0 -> "${minutes / 60} Std."
    else -> "${minutes / 60} Std. ${minutes % 60} Min."
}
