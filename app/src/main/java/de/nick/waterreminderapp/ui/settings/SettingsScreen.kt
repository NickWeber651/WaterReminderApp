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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Formfelder als String – so können wir ungültige Zwischeneingaben anzeigen
    var goalMlInput           by rememberSaveable { mutableStateOf("2000") }
    var intervalMinutesInput  by rememberSaveable { mutableStateOf("60") }
    var weekdayStartHourInput by rememberSaveable { mutableStateOf("8") }
    var weekendStartHourInput by rememberSaveable { mutableStateOf("9") }
    var endHourInput          by rememberSaveable { mutableStateOf("23") }

    // Validierungs-Ergebnis wird nur nach "Speichern"-Klick befüllt
    var validationResult by remember { mutableStateOf(SettingsValidator.ValidationResult()) }

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
                value         = goalMlInput,
                onValueChange = { goalMlInput = it },
                label         = { Text("Tagesziel (ml)") },
                isError       = validationResult.goalMlError != null,
                supportingText = validationResult.goalMlError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = intervalMinutesInput,
                onValueChange = { intervalMinutesInput = it },
                label         = { Text("Erinnerungsintervall (Minuten)") },
                isError       = validationResult.intervalMinutesError != null,
                supportingText = validationResult.intervalMinutesError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))
            Text("Zeitfenster", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value         = weekdayStartHourInput,
                onValueChange = { weekdayStartHourInput = it },
                label         = { Text("Wochentag Startzeit (Stunde 0–23)") },
                isError       = validationResult.weekdayStartHourError != null,
                supportingText = validationResult.weekdayStartHourError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = weekendStartHourInput,
                onValueChange = { weekendStartHourInput = it },
                label         = { Text("Wochenende Startzeit (Stunde 0–23)") },
                isError       = validationResult.weekendStartHourError != null,
                supportingText = validationResult.weekendStartHourError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = endHourInput,
                onValueChange = { endHourInput = it },
                label         = { Text("Endzeit (Stunde 0–23)") },
                isError       = validationResult.endHourError != null,
                supportingText = validationResult.endHourError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    // Erst Parse-Fehler abfangen – nur wenn alle Felder parsebar sind,
                    // den Validator aufrufen. So bekommt der Nutzer sofort klares Feedback.
                    val goalMl           = goalMlInput.trim().toIntOrNull()
                    val intervalMinutes  = intervalMinutesInput.trim().toIntOrNull()
                    val weekdayStartHour = weekdayStartHourInput.trim().toIntOrNull()
                    val weekendStartHour = weekendStartHourInput.trim().toIntOrNull()
                    val endHour          = endHourInput.trim().toIntOrNull()

                    val parseError = SettingsValidator.ValidationResult(
                        goalMlError           = if (goalMl == null)           "Bitte eine Zahl eingeben" else null,
                        intervalMinutesError  = if (intervalMinutes == null)  "Bitte eine Zahl eingeben" else null,
                        weekdayStartHourError = if (weekdayStartHour == null) "Bitte eine Zahl eingeben" else null,
                        weekendStartHourError = if (weekendStartHour == null) "Bitte eine Zahl eingeben" else null,
                        endHourError          = if (endHour == null)          "Bitte eine Zahl eingeben" else null,
                    )

                    if (!parseError.isValid) {
                        validationResult = parseError
                        return@Button
                    }

                    val result = SettingsValidator.validate(
                        goalMl!!, intervalMinutes!!, weekdayStartHour!!, weekendStartHour!!, endHour!!
                    )
                    validationResult = result

                    if (result.isValid) {
                        scope.launch { snackbar.showSnackbar("Gespeichert ✅") }
                        // DataStore-Speichern kommt in Commit 4 via SettingsViewModel
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
