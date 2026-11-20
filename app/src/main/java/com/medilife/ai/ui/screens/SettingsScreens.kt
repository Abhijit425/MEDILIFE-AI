package com.medilife.ai.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.medilife.ai.MainActivity
import com.medilife.ai.reminders.Notifications
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.ChipSelector
import com.medilife.ai.ui.components.ConfirmDialog
import com.medilife.ai.ui.components.Disclaimer
import com.medilife.ai.ui.components.GlassCard
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.MediTopBar
import com.medilife.ai.ui.components.PassphraseDialog
import com.medilife.ai.ui.components.SectionHeader
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette
import java.io.File

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
        }
        Switch(checked, onChange)
    }
}

@Composable
fun SettingsScreen(vm: MediViewModel, back: () -> Unit) {
    val st by vm.settings.collectAsState()
    val demo by vm.demoActive.collectAsState()
    val ctx = LocalContext.current
    val s = st ?: return
    var pendingBackupPass by remember { mutableStateOf<String?>(null) }
    var askBackup by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDemo by remember { mutableStateOf(false) }
    var emergency by remember { mutableStateOf(s.emergencyNumber) }
    var notifGranted by remember { mutableStateOf(Notifications.canPost(ctx)) }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }
    val pdfDoctor = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { it?.let { u -> vm.exportPdf(ctx, u, true) } }
    val pdfHistory = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { it?.let { u -> vm.exportPdf(ctx, u, false) } }
    val backupDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { u ->
        val p = pendingBackupPass
        if (u != null && p != null) vm.backup(ctx, u, p)
        pendingBackupPass = null
    }
    val restoreDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }
    val modelDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { u -> vm.importModel(ctx, u) } }
    val activity = ctx as? MainActivity

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Settings", back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding()) {
            SectionHeader("Appearance")
            GlassCard(Modifier.fillMaxWidth()) {
                ChipSelector(listOf("DARK", "LIGHT", "SYSTEM"), s.themeMode, { vm.setTheme(it) }) { when (it) { "DARK" -> "Dark"; "LIGHT" -> "Light"; else -> "System" } }
                ToggleRow("Reduce motion", "Turns off particles and pulsing animations (saves battery, helps motion sensitivity).", s.reduceMotion) { vm.setReduceMotion(it) }
            }

            SectionHeader("Notifications & reminders")
            GlassCard(Modifier.fillMaxWidth()) {
                if (!notifGranted && Build.VERSION.SDK_INT >= 33) {
                    Text("MediLife needs your permission to show medicine reminders. Notifications only contain the medicine name and dose and are created on your device.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow notifications") }
                }
                ToggleRow("Medicine reminders", "Daily reminders at each dose time with Taken / Skip actions.", s.remindersEnabled) { vm.setReminders(it) }
                ToggleRow("Hydration reminders", "Gentle nudges every ~2 hours, 8 am – 9 pm.", s.hydrationReminders) { vm.setHydration(it) }
                TextButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text("System notification settings") }
                Disclaimer("Android may delay reminders by a few minutes under battery optimisation. For time-critical medicines, also keep a backup alarm.")
            }

            SectionHeader("Privacy & security")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.CloudOff, Palette.tealGradient, 36.dp); Spacer(Modifier.width(10.dp))
                    Text("Local-first: no internet permission, no ads, no analytics, no uploads. Sensitive fields are encrypted with an Android Keystore key.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                val canBio = activity?.canUseBiometric() ?: false
                ToggleRow("Biometric app lock", if (canBio) "Require fingerprint/face or screen lock to open MediLife." else "Set up a screen lock or biometrics on your phone first.", s.biometricLock) { on ->
                    if (!on || canBio) vm.setBiometric(on)
                }
                OutlinedTextField(emergency, { emergency = it.filter { c -> c.isDigit() || c == '+' }.take(15) }, label = { Text("Emergency number") }, singleLine = true,
                    trailingIcon = { TextButton(onClick = { vm.setEmergencyNumber(emergency.ifBlank { "112" }); vm.toast("Emergency number saved") }) { Text("Save") } })
            }

            SectionHeader("Medi AI engine")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.AutoAwesome, Palette.primaryGradient, 36.dp); Spacer(Modifier.width(10.dp))
                    Text("Medi AI always works offline with its rule-based engine. Optionally, install a MediaPipe-compatible on-device model file (e.g. Gemma .task) for open-ended answers. Safety-critical answers stay rule-based.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                val installed = vm.llmAvailable(s.llmModelPath)
                Text(if (installed) "Installed: ${File(s.llmModelPath).name} (${File(s.llmModelPath).length() / 1_000_000} MB)" else "No local model installed", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                ToggleRow("Use on-device model", "Responses from the model are labelled 'On-device AI model'.", s.llmEnabled && installed) { vm.setLlmEnabled(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { modelDoc.launch(arrayOf("*/*")) }) { Text("Install model file") }
                    if (installed) TextButton(onClick = { vm.removeModel() }) { Text("Remove") }
                }
                Disclaimer("Large models need a recent phone with 6–8 GB RAM. If loading fails, MediLife says so and keeps using the rule engine.")
            }

            SectionHeader("Your data")
            GlassCard(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { pdfHistory.launch("MediLife-medical-history.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("Export medical history as PDF") }
                OutlinedButton(onClick = { pdfDoctor.launch("MediLife-doctor-summary.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("Export doctor visit summary (PDF)") }
                OutlinedButton(onClick = { askBackup = true }, modifier = Modifier.fillMaxWidth()) { Text("Create encrypted backup") }
                OutlinedButton(onClick = { restoreDoc.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore from backup") }
                if (demo) OutlinedButton(onClick = { confirmDemo = true }, modifier = Modifier.fillMaxWidth()) { Text("Remove all DEMO data") }
                else TextButton(onClick = { vm.loadDemo() }) { Text("Load demo data (clearly labelled)") }
                Button(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Palette.Rose)) { Text("Clear all application data") }
                Disclaimer("Backups include only your real records (never demo data) and are encrypted with your passphrase. Exports are saved only where you choose.")
            }

            SectionHeader("About MediLife AI")
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Medication, Palette.primaryGradient, 36.dp); Spacer(Modifier.width(10.dp))
                    Column {
                        Text("MediLife AI 1.0.0", style = MaterialTheme.typography.titleSmall)
                        Text("Personal health companion", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("MediLife AI provides general health information and organisation tools. It does not diagnose, prescribe, or replace professional medical advice. Its medicine and interaction data is a small curated offline dataset and is not comprehensive. In an emergency, call your local emergency number.", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Lock, Palette.tealGradient, 24.dp); Spacer(Modifier.width(6.dp))
                    IconBubble(Icons.Rounded.Science, Palette.warmGradient, 24.dp); Spacer(Modifier.width(6.dp))
                    Text("OCR: Google ML Kit (on-device) · Optional LLM: MediaPipe", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (askBackup) PassphraseDialog("Backup passphrase", true, { askBackup = false }) { pass ->
        askBackup = false; pendingBackupPass = pass; backupDoc.launch("MediLife-backup.mlbak")
    }
    restoreUri?.let { u ->
        PassphraseDialog("Restore: enter passphrase", false, { restoreUri = null }) { pass ->
            restoreUri = null; vm.restore(ctx, u, pass)
        }
    }
    if (confirmClear) ConfirmDialog("Delete everything?", "This permanently deletes all profiles, medicines, records, reports, chat history, reminders and settings from this device. Make a backup first if you need one.", "Delete all", true, { confirmClear = false }) { vm.clearAll() }
    if (confirmDemo) ConfirmDialog("Remove demo data?", "All records marked DEMO DATA will be deleted. Your own records are not affected.", "Remove", false, { confirmDemo = false }) { vm.removeDemo() }
}
