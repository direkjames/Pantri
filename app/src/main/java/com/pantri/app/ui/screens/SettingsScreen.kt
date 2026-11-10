package com.pantri.app.ui.screens

import androidx.compose.foundation.clickable
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
    val vm = pantriVm { SettingsVm(it) }
    val s by vm.settings.collectAsStateWithLifecycle()
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
            SectionLabel("About", Modifier.padding(top = 8.dp))
            PantriCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Lock, Tone.Red)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your data stays on this phone", style = MaterialTheme.typography.titleSmall)
                        Text("No account, no ads, no tracking. Pantri 1.0", style = MaterialTheme.typography.bodySmall, color = p.sub)
                    }
                }
            }
            Spacer(Modifier.size(24.dp))
        }
    }
    if (edit) BudgetDialog("Budget per cutoff", "Applies to this cutoff and every one after it. Past cutoffs keep their amounts.", s.defaultBudgetCentavos, { v -> edit = false; vm.setDefaultBudget(v) }, { edit = false })
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
