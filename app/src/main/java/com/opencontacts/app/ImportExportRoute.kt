package com.opencontacts.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.opencontacts.core.model.ImportConflictStrategy
import com.opencontacts.core.model.ImportExportHistorySummary
import com.opencontacts.core.vault.VaultSessionManager
import com.opencontacts.domain.vaults.VaultTransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun ImportExportRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
    viewModel: ImportExportViewModel = hiltViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val strategy by viewModel.strategy.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val packagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importPackageFromUri(context, uri)
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importCsvFromUri(context, uri)
    }
    val vcfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importVcfFromUri(context, uri)
    }

    SettingsScaffold(title = "Import & Export", onBack = onBack) { modifier ->
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ProgressSurface(progress)
            }
            item {
                SettingsSection(
                    title = "Import strategy",
                    subtitle = "Choose how duplicates are handled when importing exported files or large datasets.",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        ImportConflictStrategy.values().forEach { option ->
                            FilterChip(
                                selected = strategy == option,
                                onClick = { viewModel.setStrategy(option) },
                                label = { Text(option.name.lowercase().replaceFirstChar { it.titlecase() }) },
                            )
                        }
                    }
                    Text(
                        when (strategy) {
                            ImportConflictStrategy.MERGE -> "Merge updates matching contacts while preserving existing extras."
                            ImportConflictStrategy.SKIP -> "Skip matching contacts and only add new ones."
                            ImportConflictStrategy.REPLACE -> "Replace matching contact details and related notes/reminders/timeline."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SettingsSection(title = "Internal package (best round-trip)", subtitle = "Use the internal package for the most reliable export → import cycle with folders, tags, notes, reminders, timeline, and vault metadata.") {
                    ActionGridRow(
                        ActionSpec("Export package", "Recommended internal .ocpkg export", Icons.Default.FileUpload) { viewModel.exportPackage() },
                        ActionSpec("Import package", "Pick a .ocpkg document", Icons.Default.FileDownload) { packagePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    )
                }
            }
            item {
                SettingsSection(title = "Structured formats", subtitle = "Use structured JSON for readable exports, plus CSV/VCF for interoperability.") {
                    ActionGridRow(
                        ActionSpec("Export JSON", "Structured JSON export", Icons.Default.FileUpload) { viewModel.exportJson() },
                        ActionSpec("Export CSV", "Spreadsheet-friendly export", Icons.Default.Upload) { viewModel.exportCsv() },
                    )
                    SettingsSpacer()
                    ActionGridRow(
                        ActionSpec("Import CSV", "Pick a CSV document", Icons.Default.FileDownload) { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        ActionSpec("Import VCF", "Pick a VCF document", Icons.Default.Download) { vcfPicker.launch(arrayOf("text/x-vcard", "text/vcard", "*/*")) },
                    )
                    SettingsSpacer()
                    ActionGridRow(
                        ActionSpec("Export VCF", "Standard contact card format", Icons.Default.FileUpload) { viewModel.exportVcf() },
                        ActionSpec("Export Excel", "Excel-compatible workbook", Icons.Default.Upload) { viewModel.exportExcel() },
                    )
                }
            }
            item {
                SettingsSection(title = "Phone integration", subtitle = "Move data between the private vault and phone contacts when needed.") {
                    ActionGridRow(
                        ActionSpec("Import phone contacts", "Pull system contacts into the private vault", Icons.Default.PhoneAndroid) { viewModel.importFromPhone() },
                        ActionSpec("Export to phone", "Copy active vault contacts to system contacts", Icons.Default.Upload) { viewModel.exportToPhone() },
                    )
                }
            }
            item {
                SettingsSection(title = "Recent activity", subtitle = "Every run reports status and keeps its result visible until the next one completes.") {
                    if (history.isEmpty()) {
                        Text("No import or export records yet.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            history.forEach { item -> HistoryCard(item) }
                        }
                    }
                }
            }
        }
    }
}

private data class ActionSpec(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun ActionGridRow(first: ActionSpec, second: ActionSpec, hideSecond: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ActionTile(first, Modifier.weight(1f))
        if (!hideSecond) ActionTile(second, Modifier.weight(1f))
    }
}

@Composable
private fun ActionTile(spec: ActionSpec, modifier: Modifier) {
    Card(modifier = modifier, onClick = spec.onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(spec.icon, contentDescription = null)
            Text(spec.title, style = MaterialTheme.typography.titleMedium)
            Text(spec.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProgressSurface(progress: ImportExportProgressUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(progress.label, style = MaterialTheme.typography.titleMedium)
            if (progress.indeterminate) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { progress.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            Text(progress.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            progress.stats?.let { stats ->
                Text(
                    "Imported ${stats.importedCount} • Merged ${stats.mergedCount} • Skipped ${stats.skippedCount} • Failed ${stats.failedCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (stats.foldersRestored > 0 || stats.tagsRestored > 0 || stats.vaultsRestored > 0) {
                    Text(
                        "Folders ${stats.foldersRestored} • Tags ${stats.tagsRestored} • Vaults ${stats.vaultsRestored}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (progress.warnings.isNotEmpty()) {
                Text("Warnings: ${progress.warnings.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            if (progress.progress in 0f..0.999f && progress.label != "Idle") {
                Text("The task keeps running even if you leave this page.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun HistoryCard(item: ImportExportHistorySummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.operationType, style = MaterialTheme.typography.titleMedium)
            Text(item.status)
            Text("Items: ${item.itemCount}")
            Text(item.filePath)
            Text(formatTimestamp(item.createdAt), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val vaultSessionManager: VaultSessionManager,
    private val transferRepository: VaultTransferRepository,
    private val coordinator: TransferTaskCoordinator,
) : ViewModel() {
    val history: StateFlow<List<ImportExportHistorySummary>> = vaultSessionManager.activeVaultId
        .combine(vaultSessionManager.isLocked) { vaultId, locked -> vaultId to locked }
        .flatMapLatest { (vaultId, locked) ->
            if (vaultId == null || locked) flowOf(emptyList()) else transferRepository.observeImportExportHistory(vaultId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<ImportExportProgressUiState> = coordinator.importExportProgress
    private val _strategy = MutableStateFlow(ImportConflictStrategy.MERGE)
    val strategy: StateFlow<ImportConflictStrategy> = _strategy

    private fun activeVaultId(): String? = vaultSessionManager.activeVaultId.value
    fun setStrategy(value: ImportConflictStrategy) { _strategy.value = value }

    fun exportPackage() { activeVaultId()?.let(coordinator::exportPackage) }
    fun importPackage() { activeVaultId()?.let { coordinator.importPackage(it, strategy.value) } }
    fun exportJson() { activeVaultId()?.let(coordinator::exportJson) }
    fun exportCsv() { activeVaultId()?.let(coordinator::exportCsv) }
    fun exportVcf() { activeVaultId()?.let(coordinator::exportVcf) }
    fun exportExcel() { activeVaultId()?.let(coordinator::exportExcel) }
    fun importFromPhone() { activeVaultId()?.let { coordinator.importFromPhone(it, strategy.value) } }
    fun exportToPhone() { activeVaultId()?.let(coordinator::exportToPhone) }

    fun importPackageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                copyUriIntoImports(context, uri, "contacts.ocpkg")
                importPackage()
            }
        }
    }

    fun importCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                copyUriIntoImports(context, uri, "contacts.csv")
                activeVaultId()?.let { coordinator.importCsv(it, strategy.value) }
            }
        }
    }

    fun importVcfFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                copyUriIntoImports(context, uri, "contacts.vcf")
                activeVaultId()?.let { coordinator.importVcf(it, strategy.value) }
            }
        }
    }
}

private fun formatTimestamp(value: Long): String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(value))

private suspend fun copyUriIntoImports(context: Context, uri: Uri, fileName: String) {
    val dir = File(context.filesDir, "vault_imports").apply { mkdirs() }
    val target = File(dir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Unable to open selected file")
}
