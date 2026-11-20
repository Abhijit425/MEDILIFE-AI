package com.medilife.ai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.MedicalRecordEntity
import com.medilife.ai.data.ProfileEntity
import com.medilife.ai.data.RecordType
import com.medilife.ai.data.TimelineEventEntity
import com.medilife.ai.engine.EmergencyDetector
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.Avatar
import com.medilife.ai.ui.components.ChipSelector
import com.medilife.ai.ui.components.DateField
import com.medilife.ai.ui.components.DemoBadge
import com.medilife.ai.ui.components.Disclaimer
import com.medilife.ai.ui.components.GlassCard
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.KeyValue
import com.medilife.ai.ui.components.MediTopBar
import com.medilife.ai.ui.components.Pill
import com.medilife.ai.ui.components.PulsingDot
import com.medilife.ai.ui.components.SectionHeader
import com.medilife.ai.ui.components.fmtDate
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette

@Composable
fun ProfileScreen(vm: MediViewModel, go: (String) -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val ctx = LocalContext.current
    val s = snap ?: return
    val p = s.profile
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { vm.exportPdf(ctx, it, false) } }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
        item { Text("Profile", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 14.dp)) }
        item {
            GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), onClick = { go("profile_edit") }, accent = Palette.primaryGradient) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p?.photoUri ?: "", p?.name ?: "", 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p?.name?.ifBlank { null } ?: "Add your details", style = MaterialTheme.typography.titleLarge)
                        Text(listOfNotNull(p?.age?.let { "$it yrs" }, p?.gender?.ifBlank { null }, p?.bloodGroup?.ifBlank { null }?.let { "Blood $it" }).joinToString(" · ").ifBlank { "Tap to edit your profile" },
                            style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle)
                        if (p?.isDemo == true) DemoBadge()
                    }
                    Icon(Icons.Rounded.Edit, "Edit profile")
                }
            }
        }
        item {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Conditions", s.conditions.size, Modifier.weight(1f))
                Stat("Allergies", s.allergies.size, Modifier.weight(1f))
                Stat("Medicines", s.activeMedicines.size, Modifier.weight(1f))
            }
        }
        item { SectionHeader("Records") }
        item { NavRow(Icons.Rounded.History, Palette.primaryGradient, "Medical history & timeline", "Conditions, allergies, surgeries, family history") { go("history") } }
        item { NavRow(Icons.Rounded.Sos, Palette.dangerGradient, "Emergency information", "SOS call, emergency contact, critical info") { go("emergency") } }
        item { NavRow(Icons.Rounded.PictureAsPdf, Palette.tealGradient, "Export medical history (PDF)", "Saved where you choose — never uploaded") { pdf.launch("MediLife-medical-history.pdf") } }
        item { NavRow(Icons.Rounded.Settings, Palette.warmGradient, "Settings & privacy", "Theme, reminders, biometric lock, backup, data") { go("settings") } }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun Stat(label: String, n: Int, modifier: Modifier) {
    GlassCard(modifier, padding = 12.dp) {
        Text(n.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalMedi.current.subtle)
    }
}

@Composable
fun NavRow(icon: ImageVector, brush: Brush, title: String, sub: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, brush, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
        }
    }
}

@Composable
fun EditProfileScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val ctx = LocalContext.current
    var loaded by remember { mutableStateOf(false) }
    var p by remember { mutableStateOf(ProfileEntity()) }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    LaunchedEffect(snap) {
        if (!loaded && snap != null) {
            val e = snap?.profile ?: ProfileEntity()
            p = e; age = e.age?.toString() ?: ""; height = e.heightCm?.toInt()?.toString() ?: ""; weight = e.weightKg?.toString() ?: ""
            loaded = true
        }
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            p = p.copy(photoUri = it.toString())
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        MediTopBar("Edit profile", back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p.photoUri, p.name, 72.dp)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { photo.launch(arrayOf("image/*")) }) { Text("Choose photo") }
                if (p.photoUri.isNotBlank()) TextButton(onClick = { p = p.copy(photoUri = "") }) { Text("Remove") }
            }
            if (p.isDemo) Text("This is demo profile data. Saving turns it into your real profile — or remove all demo data in Settings first.", style = MaterialTheme.typography.bodySmall, color = Palette.Amber)
            OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Age") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(height, { height = it.filter(Char::isDigit).take(3) }, label = { Text("Height (cm)") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Text("Gender (optional)", style = MaterialTheme.typography.labelLarge)
            ChipSelector(listOf("", "Female", "Male", "Non-binary", "Prefer not to say"), p.gender, { p = p.copy(gender = it) }) { it.ifBlank { "Not set" } }
            Text("Blood group (optional)", style = MaterialTheme.typography.labelLarge)
            ChipSelector(listOf("", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"), p.bloodGroup, { p = p.copy(bloodGroup = it) }) { it.ifBlank { "Unknown" } }
            SectionHeader("Doctor")
            OutlinedTextField(p.doctorName, { p = p.copy(doctorName = it) }, label = { Text("Doctor name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(p.doctorSpecialty, { p = p.copy(doctorSpecialty = it) }, label = { Text("Specialty") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(p.doctorPhone, { p = p.copy(doctorPhone = it) }, label = { Text("Doctor phone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(p.clinic, { p = p.copy(clinic = it) }, label = { Text("Clinic / hospital") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            SectionHeader("Emergency contact")
            OutlinedTextField(p.emergencyName, { p = p.copy(emergencyName = it) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(p.emergencyRelation, { p = p.copy(emergencyRelation = it) }, label = { Text("Relationship") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(p.emergencyPhone, { p = p.copy(emergencyPhone = it) }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Button(onClick = {
                vm.saveProfile(p.copy(age = age.toIntOrNull(), heightCm = height.toDoubleOrNull(), weightKg = weight.replace(',', '.').toDoubleOrNull()))
                back()
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save profile") }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun MedicalHistoryScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    var addType by remember { mutableStateOf<String?>(null) }
    var editRecord by remember { mutableStateOf<MedicalRecordEntity?>(null) }
    var addEvent by remember { mutableStateOf(false) }
    val s = snap ?: return
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Medical history", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            RecordType.all.forEach { type ->
                item { SectionHeader(RecordType.label(type), "Add") { addType = type } }
                val list = s.recordsOf(type)
                if (list.isEmpty()) item { Text("None recorded", style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle) }
                items(list, key = { "r${it.id}" }) { r ->
                    GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { editRecord = r }, padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall)
                                if (r.details.isNotBlank()) Text(r.details, style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                                r.date?.let { Text(fmtDate(it), style = MaterialTheme.typography.labelSmall) }
                            }
                            if (r.isDemo) DemoBadge()
                            IconButton(onClick = { vm.deleteRecord(r) }) { Icon(Icons.Rounded.Delete, "Delete") }
                        }
                    }
                }
            }
            item { SectionHeader("Medical timeline", "Add event") { addEvent = true } }
            if (s.timeline.isEmpty()) item { Text("No events yet. Add diagnoses, procedures, check-ups and hospital visits.", style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle) }
            items(s.timeline, key = { "t${it.id}" }) { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(top = 6.dp, end = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { PulsingDot(Palette.Cyan) }
                    Column(Modifier.weight(1f)) {
                        Text(fmtDate(e.date), style = MaterialTheme.typography.labelSmall, color = Palette.Cyan)
                        Text(e.title, style = MaterialTheme.typography.titleSmall)
                        if (e.description.isNotBlank()) Text(e.description, style = MaterialTheme.typography.bodySmall)
                        Pill(e.category, Palette.Purple)
                    }
                    if (e.isDemo) DemoBadge()
                    IconButton(onClick = { vm.deleteTimeline(e) }) { Icon(Icons.Rounded.Delete, "Delete event") }
                }
            }
            item { Disclaimer("Stored encrypted on this device. Medi AI uses this information to personalise explanations.") }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
    addType?.let { t -> RecordDialog(MedicalRecordEntity(type = t, title = ""), { addType = null }) { vm.saveRecord(it); addType = null } }
    editRecord?.let { r -> RecordDialog(r, { editRecord = null }) { vm.saveRecord(it); editRecord = null } }
    if (addEvent) TimelineDialog({ addEvent = false }) { vm.saveTimeline(it); addEvent = false }
}

@Composable
private fun RecordDialog(initial: MedicalRecordEntity, onDismiss: () -> Unit, onSave: (MedicalRecordEntity) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var details by remember { mutableStateOf(initial.details) }
    var date by remember { mutableStateOf(initial.date) }
    val hint = when (initial.type) {
        RecordType.CONDITION -> "e.g. Type 2 diabetes"
        RecordType.ALLERGY -> "e.g. Penicillin, peanuts, sulfa"
        RecordType.SURGERY -> "e.g. Appendectomy"
        RecordType.FAMILY -> "e.g. Mother – hypertension"
        else -> "e.g. Telmisartan 40 mg"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(RecordType.label(initial.type)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(hint) }, singleLine = true)
                OutlinedTextField(details, { details = it }, label = { Text(if (initial.type == RecordType.ALLERGY) "Reaction (optional)" else "Details (optional)") })
                DateField("Date", date, { date = it }, allowClear = true)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(initial.copy(title = title.trim(), details = details.trim(), date = date, isDemo = initial.isDemo && title == initial.title)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TimelineDialog(onDismiss: () -> Unit, onSave: (TimelineEventEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Diagnosis") }
    var date by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timeline event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipSelector(listOf("Diagnosis", "Procedure", "Hospital", "Check-up", "Lab", "Vaccine", "General"), cat, { cat = it })
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text("Notes (optional)") })
                DateField("Date", date, { date = it })
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && date != null, onClick = { onSave(TimelineEventEntity(date = date!!, title = title.trim(), description = desc.trim(), category = cat)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmergencyScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val s = snap ?: return
    val number = settings?.emergencyNumber ?: "112"
    fun dial(n: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${n.filter { it.isDigit() || it == '+' }}"))) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Emergency information", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                Button(
                    onClick = { dial(number) },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Rose),
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                ) { Icon(Icons.Rounded.Sos, null, Modifier.size(32.dp)); Text("  Call emergency ($number)", style = MaterialTheme.typography.titleLarge) }
                Text("Opens your dialer — you confirm the call. Change the number in Settings.", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
            }
            s.profile?.takeIf { it.emergencyPhone.isNotBlank() }?.let { p ->
                item {
                    OutlinedButton(onClick = { dial(p.emergencyPhone) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(56.dp)) {
                        Icon(Icons.Rounded.Call, null); Text("  Call ${p.emergencyName} (${p.emergencyRelation})")
                    }
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth().padding(top = 14.dp), accent = Palette.dangerGradient) {
                    Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(Icons.Rounded.HealthAndSafety, Palette.dangerGradient, 36.dp); Spacer(Modifier.width(10.dp)); Text("Critical information", style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.height(8.dp))
                    KeyValue("Name", s.profile?.name?.ifBlank { null } ?: "—")
                    KeyValue("Age", s.profile?.age?.toString() ?: "—")
                    KeyValue("Blood group", s.profile?.bloodGroup?.ifBlank { null } ?: "Unknown")
                    KeyValue("Allergies", s.allergies.joinToString().ifBlank { "None recorded" })
                    KeyValue("Conditions", s.conditions.joinToString().ifBlank { "None recorded" })
                    KeyValue("Medicines", s.activeMedicines.joinToString { "${it.name} ${it.strength}" }.ifBlank { "None recorded" })
                    KeyValue("Doctor", listOf(s.profile?.doctorName, s.profile?.doctorPhone).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" })
                }
            }
            item { SectionHeader("Call for help immediately if you notice") }
            items(EmergencyDetector.warningSigns) { w -> Text("• $w", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 3.dp)) }
            item { Disclaimer("MediLife cannot detect emergencies or contact services for you. In India, 112 is the national emergency number; 108 is ambulance in many states; Tele-MANAS mental health support is 14416.") }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
