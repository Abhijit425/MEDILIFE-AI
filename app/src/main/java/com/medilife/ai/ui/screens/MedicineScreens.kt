package com.medilife.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.DoseStatus
import com.medilife.ai.data.FoodRelation
import com.medilife.ai.data.MedicineEntity
import com.medilife.ai.engine.AlertLevel
import com.medilife.ai.engine.MedicineKnowledgeBase
import com.medilife.ai.engine.SafetyAlert
import com.medilife.ai.engine.SafetyEngine
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.ChipSelector
import com.medilife.ai.ui.components.ConfirmDialog
import com.medilife.ai.ui.components.DateField
import com.medilife.ai.ui.components.DemoBadge
import com.medilife.ai.ui.components.Disclaimer
import com.medilife.ai.ui.components.EmptyState
import com.medilife.ai.ui.components.GlassCard
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.KeyValue
import com.medilife.ai.ui.components.MediTopBar
import com.medilife.ai.ui.components.Pill
import com.medilife.ai.ui.components.SectionHeader
import com.medilife.ai.ui.components.TimePickDialog
import com.medilife.ai.ui.components.fmtDate
import com.medilife.ai.ui.components.fmtDateTime
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette
import java.time.LocalTime

fun levelColor(l: AlertLevel): Color = when (l) { AlertLevel.HIGH -> Palette.Rose; AlertLevel.MODERATE -> Palette.Amber; AlertLevel.INFO -> Palette.Cyan }

@Composable
fun MedicinesScreen(vm: MediViewModel, go: (String) -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val alerts by vm.alerts.collectAsState()
    val s = snap ?: return
    var showInactive by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
            item { Text("Medicines", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 14.dp)) }
            item {
                val high = alerts.count { it.level == AlertLevel.HIGH }
                GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), onClick = { go("interactions") }, accent = if (high > 0) Palette.dangerGradient else Palette.tealGradient) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Shield, if (high > 0) Palette.dangerGradient else Palette.tealGradient)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI Medicine Safety Engine", style = MaterialTheme.typography.titleSmall)
                            Text(if (alerts.isEmpty()) "No alerts from offline checks" else "${alerts.size} alert(s) · $high high priority — tap for details", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                        }
                        Pill("OFFLINE", Palette.Cyan)
                    }
                }
            }
            val active = s.medicines.filter { it.active }
            val inactive = s.medicines.filter { !it.active }
            item { SectionHeader("Active (${active.size})") }
            if (active.isEmpty()) item { EmptyState(Icons.Rounded.Medication, "No medicines yet", "Add your prescribed medicines to get reminders and safety checks.", "Add medicine") { go("medicine_edit/0") } }
            items(active, key = { it.id }) { m -> MedicineRow(m, alerts.filter { m.id in it.medicineIds }) { go("medicine/${m.id}") } }
            if (inactive.isNotEmpty()) {
                item { SectionHeader("Previous / inactive (${inactive.size})", if (showInactive) "Hide" else "Show") { showInactive = !showInactive } }
                if (showInactive) items(inactive, key = { it.id }) { m -> MedicineRow(m, emptyList()) { go("medicine/${m.id}") } }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { go("medicine_edit/0") },
            icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add medicine") },
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
        )
    }
}

@Composable
private fun MedicineRow(m: MedicineEntity, alerts: List<SafetyAlert>, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.Medication, if (m.active) Palette.primaryGradient else Palette.warmGradient, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${m.name} ${m.strength}".trim(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    if (m.isDemo) { Spacer(Modifier.width(6.dp)); DemoBadge() }
                }
                Text(listOf(m.dosage, m.frequency, m.foodRelation.takeIf { it != FoodRelation.ANY }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Details not recorded" },
                    style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                if (m.times.isNotBlank()) Text("⏰ ${DoseSchedule.parseTimes(m.times).joinToString()}", style = MaterialTheme.typography.labelSmall)
            }
            alerts.maxByOrNull { it.level.rank }?.let { a -> Icon(Icons.Rounded.Warning, a.title, tint = levelColor(a.level)) }
        }
    }
}

@Composable
fun MedicineEditScreen(vm: MediViewModel, id: Long, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val existing = snap?.medicines?.firstOrNull { it.id == id }
    var loaded by remember { mutableStateOf(id == 0L) }
    var name by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("Tablet") }
    var frequency by remember { mutableStateOf("Once daily") }
    var times by remember { mutableStateOf(listOf(LocalTime.of(9, 0))) }
    var start by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var end by remember { mutableStateOf<Long?>(null) }
    var expiry by remember { mutableStateOf<Long?>(null) }
    var instructions by remember { mutableStateOf("") }
    var food by remember { mutableStateOf(FoodRelation.ANY) }
    var doctor by remember { mutableStateOf("") }
    var reminders by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf(true) }
    var timeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!loaded && existing != null) {
            name = existing.name; strength = existing.strength; dosage = existing.dosage; form = existing.form; frequency = existing.frequency
            times = DoseSchedule.parseTimes(existing.times); start = existing.startDate; end = existing.endDate; expiry = existing.expiryDate
            instructions = existing.instructions; food = existing.foodRelation; doctor = existing.prescribedBy; reminders = existing.remindersOn; active = existing.active
            loaded = true
        }
    }
    val match = MedicineKnowledgeBase.find(name)
    val freqs = listOf("Once daily", "Twice daily", "Three times daily", "Four times daily", "Every other day", "Weekly", "As needed")

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        MediTopBar(if (id == 0L) "Add medicine" else "Edit medicine", back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Disclaimer("Enter details exactly as written on your prescription or label. MediLife never calculates or changes doses.")
            OutlinedTextField(name, { name = it }, label = { Text("Medicine name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(if (name.isBlank()) "Generic or brand name" else match?.let { "Recognised: ${it.displayName} (${it.drugClass})" } ?: "Not in offline library — safety checks will be limited") })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(strength, { strength = it }, label = { Text("Strength (e.g. 500 mg)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(dosage, { dosage = it }, label = { Text("Dose as prescribed") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text("Form", style = MaterialTheme.typography.labelLarge)
            ChipSelector(listOf("Tablet", "Capsule", "Syrup", "Injection", "Inhaler", "Drops", "Cream", "Other"), form, { form = it })
            Text("Frequency", style = MaterialTheme.typography.labelLarge)
            ChipSelector(freqs, frequency, { f ->
                frequency = f
                times = when (f) {
                    "Once daily", "Every other day", "Weekly" -> listOf(LocalTime.of(9, 0))
                    "Twice daily" -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
                    "Three times daily" -> listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
                    "Four times daily" -> listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0))
                    else -> emptyList()
                }
            })
            if (frequency == "Every other day" || frequency == "Weekly") Text("Reminders are daily — log the days you don't take it as skipped, or switch reminders off.", style = MaterialTheme.typography.bodySmall)
            Text("Reminder times (match your prescription)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                times.forEach { t -> InputChip(selected = true, onClick = { times = times - t }, label = { Text("$t  ✕") }) }
                OutlinedButton(onClick = { timeDialog = true }) { Text("+ Time") }
            }
            Text("When to take", style = MaterialTheme.typography.labelLarge)
            ChipSelector(FoodRelation.all, food, { food = it })
            DateField("Start", start, { start = it })
            DateField("End (optional)", end, { end = it }, allowClear = true)
            DateField("Pack expiry (optional)", expiry, { expiry = it }, allowClear = true)
            OutlinedTextField(instructions, { instructions = it }, label = { Text("Doctor's instructions") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(doctor, { doctor = it }, label = { Text("Prescribed by") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Reminders", Modifier.weight(1f)); Switch(reminders, { reminders = it }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Currently taking", Modifier.weight(1f)); Switch(active, { active = it }) }
            Button(
                enabled = name.isNotBlank() && start != null,
                onClick = {
                    vm.saveMedicine(MedicineEntity(
                        id = id, name = name.trim(), strength = strength.trim(), dosage = dosage.trim(), form = form, frequency = frequency,
                        times = times.sorted().joinToString(",") { "%02d:%02d".format(it.hour, it.minute) }, startDate = start!!, endDate = end, expiryDate = expiry,
                        instructions = instructions.trim(), foodRelation = food, prescribedBy = doctor.trim(), remindersOn = reminders, active = active,
                        isDemo = existing?.isDemo ?: false
                    ))
                    back()
                }, modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Save medicine") }
            Spacer(Modifier.height(30.dp))
        }
    }
    if (timeDialog) TimePickDialog(LocalTime.of(9, 0), { timeDialog = false }) { times = (times + it).distinct().sorted(); timeDialog = false }
}

@Composable
fun MedicineDetailScreen(vm: MediViewModel, id: Long, back: () -> Unit, go: (String) -> Unit) {
    val med by vm.medicineFlow(id).collectAsState(initial = null)
    val logs by vm.doseLogsFor(id).collectAsState(initial = emptyList())
    val alerts by vm.alerts.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val m = med
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar(m?.name ?: "Medicine", back) {
            IconButton(onClick = { go("medicine_edit/$id") }) { Icon(Icons.Rounded.Edit, "Edit") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
        }
        if (m == null) return@Column
        val info = MedicineKnowledgeBase.find(m.name)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Palette.primaryGradient) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Medication, Palette.primaryGradient, 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${m.name} ${m.strength}", style = MaterialTheme.typography.titleLarge)
                            Text(info?.drugClass ?: "Not in offline library", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                        }
                        if (m.isDemo) DemoBadge()
                    }
                    Spacer(Modifier.height(10.dp))
                    KeyValue("Dose", m.dosage.ifBlank { "Not recorded" })
                    KeyValue("Frequency", m.frequency.ifBlank { "Not recorded" })
                    KeyValue("Times", DoseSchedule.parseTimes(m.times).joinToString().ifBlank { "As needed" })
                    KeyValue("Food", m.foodRelation)
                    KeyValue("Course", "${fmtDate(m.startDate)} → ${m.endDate?.let { fmtDate(it) } ?: "ongoing"}")
                    m.expiryDate?.let { KeyValue("Expiry", fmtDate(it)) }
                    if (m.prescribedBy.isNotBlank()) KeyValue("Prescriber", m.prescribedBy)
                    if (m.instructions.isNotBlank()) KeyValue("Instructions", m.instructions)
                }
            }
            val mine = alerts.filter { m.id in it.medicineIds }
            if (mine.isNotEmpty()) {
                item { SectionHeader("Safety alerts") }
                items(mine) { AlertCard(it) }
            }
            item { SectionHeader("About this medicine") }
            item {
                if (info == null) EmptyState(Icons.Rounded.Info, "No offline information", "MediLife's curated library doesn't include \"${m.name}\". Ask your pharmacist about uses, side effects and interactions.")
                else GlassCard(Modifier.fillMaxWidth()) {
                    InfoBlock("What it's generally used for", info.uses)
                    InfoBlock("How it works", info.howItWorks)
                    InfoBlock("Common side effects", info.sideEffects.joinToString("\n") { "• $it" })
                    InfoBlock("Important precautions", info.precautions.joinToString("\n") { "• $it" })
                    InfoBlock("Known interactions (not complete)", info.interactions.joinToString("\n") { "• $it" })
                    InfoBlock("Storage", info.storage)
                    InfoBlock("If you miss a dose", info.missedDose)
                    info.supervision?.let { InfoBlock("Supervision", it) }
                    Disclaimer(MedicineKnowledgeBase.DISCLAIMER)
                }
            }
            item { SectionHeader("Dose history") }
            if (logs.isEmpty()) item { Text("No doses logged yet.", style = MaterialTheme.typography.bodyMedium) }
            items(logs.take(20)) { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(fmtDateTime(l.scheduledAt), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(if (l.status == DoseStatus.TAKEN) "Taken" else "Skipped", color = if (l.status == DoseStatus.TAKEN) Palette.Teal else Palette.Amber, fontWeight = FontWeight.SemiBold)
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
    if (confirmDelete && m != null) ConfirmDialog("Delete ${m.name}?", "This removes the medicine and its dose history. To record that you stopped it, edit it and switch off \"Currently taking\" instead.", "Delete", true, { confirmDelete = false }) {
        vm.deleteMedicine(id); back()
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun AlertCard(a: SafetyAlert) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = levelColor(a.level))
            Spacer(Modifier.width(8.dp))
            Text(a.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Pill(a.level.label.uppercase(), levelColor(a.level))
        }
        Spacer(Modifier.height(6.dp))
        Text("Why: ${a.why}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("What to do: ${a.action}", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
    }
}

@Composable
fun InteractionCheckerScreen(vm: MediViewModel, back: () -> Unit) {
    val alerts by vm.alerts.collectAsState()
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        MediTopBar("Medicine interaction checker", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Check two medicines", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(a, { a = it }, label = { Text("First medicine") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(b, { b = it }, label = { Text("Second medicine") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val x = MedicineKnowledgeBase.find(a); val y = MedicineKnowledgeBase.find(b)
                        result = when {
                            x == null || y == null -> "Not found in the offline library: ${listOfNotNull(if (x == null) a else null, if (y == null) b else null).joinToString()}. This combination could not be checked — ask your pharmacist."
                            else -> SafetyEngine.checkPair(x, y).let { hits ->
                                if (hits.isEmpty()) "No interaction between ${x.displayName} and ${y.displayName} in MediLife's small offline dataset. That does not mean the combination is safe for you."
                                else hits.joinToString("\n\n") { (l, tw) -> "⚠️ ${l.label}: ${tw.first}\n${tw.second}" } + "\n\nDiscuss with your pharmacist or doctor. Don't stop prescribed medicines on your own."
                            }
                        }
                    }, enabled = a.isNotBlank() && b.isNotBlank()) { Text("Check") }
                    result?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item { SectionHeader("Alerts for your medicine list") }
            if (alerts.isEmpty()) item { Text("No alerts found by the offline checks.", style = MaterialTheme.typography.bodyMedium) }
            items(alerts) { AlertCard(it) }
            item { Disclaimer(SafetyEngine.NOT_COMPREHENSIVE) }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
