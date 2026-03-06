package de.nick.waterreminderapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nick.waterreminderapp.data.DataStoreIntakeRepository
import de.nick.waterreminderapp.data.SettingsStore

private const val MAX_VISIBLE_ENTRIES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context  = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // ── ViewModel (Factory mit remember, damit bei Recomposition keine neuen Instanzen entstehen)
    val factory = remember {
        HomeViewModel.Factory(
            repository    = DataStoreIntakeRepository.create(context),
            settingsStore = SettingsStore(context)
        )
    }
    val vm: HomeViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }

    // ── Bottom Sheet State ────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboard   = LocalSoftwareKeyboardController.current

    // ── Scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("💧 Water Reminder") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menü öffnen")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Einstellungen") },
                            onClick = { menuExpanded = false; onNavigateToSettings() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openAddSheet() }) {
                Icon(Icons.Filled.Add, contentDescription = "Eintrag hinzufügen")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Progress-Card ─────────────────────────────────────────────
            HydrationProgressCard(
                totalMl = state.totalMl,
                goalMl  = state.goalMl
            )

            // ── Historien-Bereich ─────────────────────────────────────────
            HistorySection(
                entries        = state.entries,
                onDelete       = { vm.deleteEntry(it) },
                onShowMore     = onNavigateToHistory
            )

            Spacer(Modifier.height(80.dp)) // Platz für FAB
        }
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────
    if (state.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { vm.closeAddSheet() },
            sheetState       = sheetState
        ) {
            AddEntrySheetContent(
                inputText  = state.inputText,
                inputError = state.inputError,
                onInput    = { vm.onInputChange(it) },
                onConfirm  = {
                    keyboard?.hide()
                    vm.confirmAdd()
                },
                onDismiss  = {
                    keyboard?.hide()
                    vm.closeAddSheet()
                }
            )
        }
    }
}

// ── Historien-Sektion ─────────────────────────────────────────────────────────

@Composable
private fun HistorySection(
    entries: List<de.nick.waterreminderapp.data.WaterEntry>,
    onDelete: (String) -> Unit,
    onShowMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Heute getrunken",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (entries.isEmpty()) {
            // ── Empty State ───────────────────────────────────────────────
            Text(
                text  = "Noch keine Einträge",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            // ── Einträge (max. MAX_VISIBLE_ENTRIES) ───────────────────────
            val visible = entries.takeLast(MAX_VISIBLE_ENTRIES).reversed()
            visible.forEachIndexed { index, entry ->
                EntryRow(
                    amountMl = entry.amountMl,
                    onDelete = { onDelete(entry.id) }
                )
                if (index < visible.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            // ── "Mehr anzeigen" ───────────────────────────────────────────
            if (entries.size > MAX_VISIBLE_ENTRIES) {
                TextButton(
                    onClick  = onShowMore,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Mehr anzeigen (${entries.size - MAX_VISIBLE_ENTRIES} weitere)")
                }
            }
        }
    }
}

// ── Einzelner Eintrag ─────────────────────────────────────────────────────────

@Composable
private fun EntryRow(
    amountMl: Int,
    onDelete: () -> Unit
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = "💧 $amountMl ml",
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector        = Icons.Filled.Delete,
                contentDescription = "Eintrag löschen",
                tint               = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ── Bottom Sheet Inhalt ───────────────────────────────────────────────────────

@Composable
private fun AddEntrySheetContent(
    inputText:  String,
    inputError: String?,
    onInput:    (String) -> Unit,
    onConfirm:  () -> Unit,
    onDismiss:  () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text  = "Wasser hinzufügen",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value         = inputText,
            onValueChange = onInput,
            label         = { Text("Menge in ml") },
            suffix        = { Text("ml") },
            isError       = inputError != null,
            supportingText = inputError?.let { { Text(it) } },
            singleLine    = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier      = Modifier.fillMaxWidth()
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Abbrechen")
            }
            Button(
                onClick  = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Text("Hinzufügen")
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
