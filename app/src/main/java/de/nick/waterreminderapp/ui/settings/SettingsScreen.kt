package de.nick.waterreminderapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nick.waterreminderapp.data.SettingsStore
import de.nick.waterreminderapp.scheduler.ContextReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
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

    // Einmal-Events abhören: Snackbar nur anzeigen wenn wirklich gespeichert
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Tagesziel & Intervall", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value           = uiState.goalMlInput,
                onValueChange   = vm::onGoalMlChange,
                label           = { Text("Tagesziel (ml)") },
                isError         = v.goalMlError != null,
                supportingText  = v.goalMlError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value           = uiState.intervalMinutesInput,
                onValueChange   = vm::onIntervalMinutesChange,
                label           = { Text("Erinnerungsintervall (Minuten)") },
                isError         = v.intervalMinutesError != null,
                supportingText  = v.intervalMinutesError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))
            Text("Zeitfenster", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value           = uiState.weekdayStartHourInput,
                onValueChange   = vm::onWeekdayStartHourChange,
                label           = { Text("Wochentag Startzeit (Stunde 0–23)") },
                isError         = v.weekdayStartHourError != null,
                supportingText  = v.weekdayStartHourError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value           = uiState.weekendStartHourInput,
                onValueChange   = vm::onWeekendStartHourChange,
                label           = { Text("Wochenende Startzeit (Stunde 0–23)") },
                isError         = v.weekendStartHourError != null,
                supportingText  = v.weekendStartHourError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value           = uiState.endHourInput,
                onValueChange   = vm::onEndHourChange,
                label           = { Text("Endzeit (Stunde 0–23)") },
                isError         = v.endHourError != null,
                supportingText  = v.endHourError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = vm::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
