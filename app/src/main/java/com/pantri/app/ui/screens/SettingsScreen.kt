package com.pantri.app.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.ui.platform.LocalContext
import com.pantri.app.PantriApp
import com.pantri.app.data.Settings
import com.pantri.app.ui.components.Choice
import com.pantri.app.ui.components.PantriDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pantri.app.domain.pesos
import com.pantri.app.ui.SettingsVm
import com.pantri.app.ui.components.BudgetDialog
import com.pantri.app.ui.components.IconTile
import com.pantri.app.ui.components.PantriCard
import com.pantri.app.ui.components.Pill
import com.pantri.app.ui.components.SectionLabel
import com.pantri.app.ui.components.TopBar
import com.pantri.app.ui.pantriVm
import com.pantri.app.ui.theme.Tone
import com.pantri.app.ui.theme.pantri

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val backup = (context.applicationContext as PantriApp).backup
    val vm = pantriVm { SettingsVm(it, backup) }
    val s by vm.settings.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Keep access after the app closes; without this the folder is forgotten at the next start.
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            vm.setFolder(uri.toString())
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.pick(uri) }
    val p = MaterialTheme.pantri
    var edit by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Settings", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Appearance")
            PantriCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                SettingRow(Icons.Rounded.DarkMode, Tone.Yellow, "Dark mode", "Pantri starts in light mode", checked = s.darkMode, onChecked = vm::setDark)
                SettingRow(Icons.Rounded.Vibration, Tone.Green, "Haptics", "Small taps when you check things off", checked = s.haptics, onChecked = vm::setHaptics)
            }
            SectionLabel("Budget", Modifier.padding(top = 8.dp))
            PantriCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { edit = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Wallet, Tone.Green)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Budget per cutoff", style = MaterialTheme.typography.titleSmall)
                        Text("Philippine peso. Tap to change.", style = MaterialTheme.typography.bodySmall, color = p.sub)
                    }
                    Pill(pesos(s.defaultBudgetCentavos), Tone.Green)
                }
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.CalendarMonth, Tone.Orange)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cutoff days", style = MaterialTheme.typography.titleSmall)
                        Text("Ends on the 15th and the last day of the month.", style = MaterialTheme.typography.bodySmall, color = p.sub)
                    }
                }
            }
            SectionLabel("Backup", Modifier.padding(top = 8.dp))
            BackupCard(
                s, busy, onFolder = { pickFolder.launch(null) },
                onNow = { if (s.backupFolder == null) pickFolder.launch(null) else vm.backupNow() },
                onSchedule = vm::setSchedule, onRestore = { pickFile.launch(arrayOf("*/*")) },
            )
            SectionLabel("About", Modifier.padding(top = 8.dp))
            PantriCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Lock, Tone.Red)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your data stays on this phone", style = MaterialTheme.typography.titleSmall)
                        Text("No account, no ads, no tracking. Pantri 1.1", style = MaterialTheme.typography.bodySmall, color = p.sub)
                    }
                }
            }
            Spacer(Modifier.size(24.dp))
        }
    }
    pending?.let { d ->
        PantriDialog("Restore this backup?", vm::cancelRestore, "Restore", vm::confirmRestore) {
            val day = java.time.Instant.ofEpochMilli(d.createdAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
            fun count(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
            Text(
                "Saved $day: ${count(d.lists.count { !it.isTemplate }, "list", "lists")}, ${count(d.recipes.size, "recipe", "recipes")}, " +
                    "${count(d.stores.size, "store", "stores")}, ${count(d.meals.size, "planned meal", "planned meals")}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Everything in Pantri now is replaced by this backup.", style = MaterialTheme.typography.bodyMedium, color = p.sub)
            Text(
                if (s.backupFolder != null) "A safety copy of what you have now is saved to your backup folder first." else "No backup folder is set, so there is no safety copy of what you have now.",
                style = MaterialTheme.typography.bodySmall, color = p.sub,
            )
        }
    }
    if (edit) BudgetDialog("Budget per cutoff", "Applies to this cutoff and every one after it. Past cutoffs keep their amounts.", s.defaultBudgetCentavos, { v -> edit = false; vm.setDefaultBudget(v) }, { edit = false })
}

private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupCard(s: Settings, busy: Boolean, onFolder: () -> Unit, onNow: () -> Unit, onSchedule: (Int, Int) -> Unit, onRestore: () -> Unit) {
    val p = MaterialTheme.pantri
    val folder = s.backupFolder?.let { it.toUri().lastPathSegment?.substringAfter(':') }
    val last = if (s.lastBackupAt > 0) java.time.Instant.ofEpochMilli(s.lastBackupAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM, h:mm a")) else "never"
    val mode = if (s.backupWeekday > 0) 2 else if (s.backupDays > 0) 1 else 0
    PantriCard(Modifier.fillMaxWidth(), padding = 6.dp) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onFolder).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.Folder, Tone.Yellow)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Backup folder", style = MaterialTheme.typography.titleSmall)
                Text(folder ?: "Not chosen yet. Tap to choose one.", style = MaterialTheme.typography.bodySmall, color = p.sub)
            }
        }
        Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button, onClick = onNow).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.CloudUpload, Tone.Green)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (busy) "Working..." else "Back up now", style = MaterialTheme.typography.titleSmall)
                Text("Last backup: $last", style = MaterialTheme.typography.bodySmall, color = p.sub)
                if (s.backupError != null) Text(s.backupError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text("Automatic backup", style = MaterialTheme.typography.titleSmall)
            Text("Pantri backs up when you open it and one is due. The newest 7 are kept.", style = MaterialTheme.typography.bodySmall, color = p.sub)
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Off", mode == 0) { onSchedule(0, 0) }
                Choice("Every few days", mode == 1) { onSchedule(if (s.backupDays > 0) s.backupDays else 3, 0) }
                Choice("Weekly on a day", mode == 2) { onSchedule(0, if (s.backupWeekday > 0) s.backupWeekday else 1) }
            }
            if (mode == 1) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5, 7, 14, 30).forEach { n -> Choice(if (n == 1) "Every day" else "Every $n days", s.backupDays == n) { onSchedule(n, 0) } }
            }
            if (mode == 2) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weekdays.forEachIndexed { i, d -> Choice(d, s.backupWeekday == i + 1) { onSchedule(0, i + 1) } }
            }
            if (mode != 0 && s.backupFolder == null) Text("Choose a backup folder above to start.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }
        Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button, onClick = onRestore).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.Restore, Tone.Red)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Restore from a backup", style = MaterialTheme.typography.titleSmall)
                Text("Pick a Pantri backup file. It replaces what is in the app now.", style = MaterialTheme.typography.bodySmall, color = p.sub)
            }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, tone: Tone, title: String, body: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val p = MaterialTheme.pantri
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChecked(!checked) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, tone)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = p.sub)
        }
        Switch(checked, onChecked, colors = SwitchDefaults.colors(checkedTrackColor = p.green, checkedThumbColor = Color.White, uncheckedTrackColor = p.line, uncheckedBorderColor = p.muted, uncheckedThumbColor = p.muted))
    }
}
