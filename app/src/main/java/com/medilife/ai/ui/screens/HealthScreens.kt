package com.medilife.ai.ui.screens

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sick
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.AppointmentEntity
import com.medilife.ai.data.MeasureType
import com.medilife.ai.data.SymptomEntity
import com.medilife.ai.engine.HealthTipsEngine
import com.medilife.ai.engine.MediAiEngine
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.AddMeasurementDialog
import com.medilife.ai.ui.components.ChipSelector
import com.medilife.ai.ui.components.DateField
import com.medilife.ai.ui.components.DemoBadge
import com.medilife.ai.ui.components.Disclaimer
import com.medilife.ai.ui.components.EmptyState
import com.medilife.ai.ui.components.GlassCard
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.LineChart
import com.medilife.ai.ui.components.MediTopBar
import com.medilife.ai.ui.components.Pill
import com.medilife.ai.ui.components.SectionHeader
import com.medilife.ai.ui.components.SourceBadge
import com.medilife.ai.ui.components.TimeField
import com.medilife.ai.ui.components.fmtDate
import com.medilife.ai.ui.components.fmtDateTime
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun HealthScreen(vm: MediViewModel, go: (String) -> Unit) {
    var add by remember { mutableStateOf(false) }
    val tiles = listOf(
        Triple("Health trends", Icons.AutoMirrored.Rounded.ShowChart, "trends"),
        Triple("Symptom journal", Icons.Rounded.Sick, "symptoms"),
        Triple("Lab reports", Icons.Rounded.Science, "reports"),
        Triple("Appointments", Icons.Rounded.CalendarMonth, "appointments"),
        Triple("Body explorer", Icons.Rounded.AccessibilityNew, "body"),
        Triple("Health tips", Icons.Rounded.Lightbulb, "tips"),
        Triple("Emergency info", Icons.Rounded.Sos, "emergency")
    )
    val brushes = listOf(Palette.primaryGradient, Palette.warmGradient, Palette.tealGradient, Palette.primaryGradient, Palette.tealGradient, Palette.warmGradient, Palette.dangerGradient)
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
        item { Text("Health", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 14.dp)) }
        item {
            GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), onClick = { add = true }, accent = Palette.primaryGradient) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.MonitorHeart, Palette.primaryGradient, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Log a measurement", style = MaterialTheme.typography.titleMedium)
                        Text("Blood pressure, heart rate, SpO₂, weight, glucose, sleep, activity", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                    }
                    Icon(Icons.Rounded.Add, null)
                }
            }
        }
        item { SectionHeader("Tools") }
        tiles.chunked(2).forEachIndexed { rowIdx, row ->
            item {
                Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { i, (title, icon, route) ->
                        GlassCard(Modifier.weight(1f), onClick = { go(route) }, padding = 14.dp) {
                            IconBubble(icon, brushes[rowIdx * 2 + i], 40.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(title, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Disclaimer("Readings you enter are labelled MANUAL. Device-sourced readings are labelled DEVICE. MediLife never fabricates measurements.") }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (add) AddMeasurementDialog(MeasureType.BLOOD_PRESSURE, { add = false }) { vm.saveMeasurement(it); add = false }
}

@Composable
fun TrendsScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    var type by remember { mutableStateOf(MeasureType.BLOOD_PRESSURE) }
    var add by remember { mutableStateOf(false) }
    val s = snap ?: return
    val list = s.measurements.filter { it.type == type }.sortedBy { it.timestamp }.takeLast(30)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Health trends", back) { IconButton(onClick = { add = true }) { Icon(Icons.Rounded.Add, "Add reading") } }
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item { ChipSelector(MeasureType.all, type, { type = it }) { MeasureType.label(it) } }
            item {
                GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(MeasureType.label(type), style = MaterialTheme.typography.titleMedium)
                    if (list.isEmpty()) Text("No readings yet.", style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle)
                    else {
                        Text("${list.size} readings · ${fmtDate(list.first().timestamp)} – ${fmtDate(list.last().timestamp)}", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                        Spacer(Modifier.height(12.dp))
                        LineChart(
                            list.map { it.value.toFloat() },
                            secondValues = if (type == MeasureType.BLOOD_PRESSURE) list.map { (it.value2 ?: 0.0).toFloat() } else null,
                            color = if (type == MeasureType.BLOOD_PRESSURE) Palette.Purple else Palette.Cyan,
                            secondColor = Palette.Cyan,
                            description = "${MeasureType.label(type)} chart"
                        )
                        if (type == MeasureType.BLOOD_PRESSURE) Text("Purple = systolic · Cyan = diastolic", style = MaterialTheme.typography.labelSmall)
                        val avg = list.map { it.value }.average()
                        Text("Average: ${MediAiEngine.fmtMeasure(type, avg, if (type == MeasureType.BLOOD_PRESSURE) list.mapNotNull { it.value2 }.average() else null)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { SectionHeader("History") }
            items(list.reversed(), key = { it.id }) { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(MediAiEngine.fmtMeasure(m.type, m.value, m.value2), style = MaterialTheme.typography.titleSmall)
                        Text(fmtDateTime(m.timestamp) + if (m.note.isNotBlank()) " · ${m.note}" else "", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                    }
                    if (m.isDemo) DemoBadge() else SourceBadge(m.source)
                    IconButton(onClick = { vm.deleteMeasurement(m) }) { Icon(Icons.Rounded.Delete, "Delete reading") }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
    if (add) AddMeasurementDialog(type, { add = false }) { vm.saveMeasurement(it); add = false }
}

@Composable
fun SymptomJournalScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    var name by remember { mutableStateOf("") }
    var sev by remember { mutableStateOf(4f) }
    var notes by remember { mutableStateOf("") }
    var at by remember { mutableStateOf(System.currentTimeMillis()) }
    val s = snap ?: return
    val emergency = com.medilife.ai.engine.EmergencyDetector.detect("$name $notes")
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        MediTopBar("Symptom journal", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Log a symptom", style = MaterialTheme.typography.titleMedium)
                    ChipSelector(listOf("Headache", "Fatigue", "Dizziness", "Nausea", "Cough", "Fever", "Pain", "Breathlessness", "Anxiety", "Poor sleep"), name, { name = it })
                    OutlinedTextField(name, { name = it }, label = { Text("Symptom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Severity: ${sev.toInt()}/10", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    Slider(sev, { sev = it }, valueRange = 1f..10f, steps = 8)
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (triggers, timing…)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { DateField("Date", at, { it?.let { d -> at = d } }) }
                    TimeField("Time", at) { at = it }
                    emergency?.let {
                        GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp), accent = Palette.dangerGradient) {
                            Text("⚠️ ${it.title}", style = MaterialTheme.typography.titleSmall, color = Palette.Rose)
                            Text(it.advice, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = { vm.saveSymptom(SymptomEntity(name = name.trim(), severity = sev.toInt(), notes = notes.trim(), timestamp = at)); name = ""; notes = ""; sev = 4f },
                        enabled = name.isNotBlank(), modifier = Modifier.padding(top = 8.dp)) { Text("Save") }
                }
            }
            val recent = s.symptoms.filter { it.timestamp > System.currentTimeMillis() - 30L * 86_400_000 }
            if (recent.isNotEmpty()) item {
                SectionHeader("Last 30 days")
                GlassCard(Modifier.fillMaxWidth()) {
                    recent.groupBy { it.name }.entries.sortedByDescending { it.value.size }.forEach { (n, l) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text("${l.size}× · avg ${"%.1f".format(l.map { it.severity }.average())}/10", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (recent.size >= 3) {
                        Spacer(Modifier.height(8.dp))
                        LineChart(recent.sortedBy { it.timestamp }.map { it.severity.toFloat() }, color = Palette.Pink, description = "Symptom severity trend")
                    }
                }
            }
            item { SectionHeader("Entries") }
            if (s.symptoms.isEmpty()) item { EmptyState(Icons.Rounded.Sick, "No symptoms logged", "Logging symptoms helps you and your doctor spot patterns.") }
            items(s.symptoms, key = { it.id }) { x ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${x.name} · ${x.severity}/10", style = MaterialTheme.typography.titleSmall)
                        Text(fmtDateTime(x.timestamp) + if (x.notes.isNotBlank()) " · ${x.notes}" else "", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                    }
                    if (x.isDemo) DemoBadge()
                    IconButton(onClick = { vm.deleteSymptom(x) }) { Icon(Icons.Rounded.Delete, "Delete") }
                }
            }
            item { Disclaimer("The journal records what you tell it; it doesn't diagnose. Seek care for new, severe or worsening symptoms.") }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun AppointmentsScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    val s = snap ?: return
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Appointments", back) { IconButton(onClick = { adding = true }) { Icon(Icons.Rounded.Add, "Add appointment") } }
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Palette.tealGradient) {
                    Text("Doctor visit summary", style = MaterialTheme.typography.titleMedium)
                    Text("A one-page summary of your conditions, allergies, medicines, readings, latest lab results and symptoms to share with your doctor.", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val text = vm.doctorSummary()
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share doctor summary"))
                        }
                    }) { Icon(Icons.Rounded.Share, null); Text("  Share summary") }
                }
            }
            val upcoming = s.appointments.filter { it.dateTime >= now }
            val past = s.appointments.filter { it.dateTime < now }.reversed()
            item { SectionHeader("Upcoming") }
            if (upcoming.isEmpty()) item { EmptyState(Icons.Rounded.CalendarMonth, "No upcoming appointments", "Add check-ups so they show on your dashboard.", "Add appointment") { adding = true } }
            items(upcoming, key = { it.id }) { ApptRow(it) { vm.deleteAppointment(it) } }
            if (past.isNotEmpty()) {
                item { SectionHeader("Past") }
                items(past, key = { it.id }) { ApptRow(it) { vm.deleteAppointment(it) } }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
    if (adding) AddAppointmentDialog({ adding = false }) { vm.saveAppointment(it); adding = false }
}

@Composable
private fun ApptRow(a: AppointmentEntity, onDelete: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.CalendarMonth, Palette.tealGradient, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.purpose.ifBlank { "Appointment" }, style = MaterialTheme.typography.titleSmall)
                Text("${a.doctor} · ${fmtDateTime(a.dateTime)}", style = MaterialTheme.typography.bodySmall)
                if (a.location.isNotBlank()) Text(a.location, style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
            }
            if (a.isDemo) DemoBadge()
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete") }
        }
    }
}

@Composable
private fun AddAppointmentDialog(onDismiss: () -> Unit, onSave: (AppointmentEntity) -> Unit) {
    var doctor by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var at by remember { mutableStateOf(System.currentTimeMillis() + 86_400_000) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New appointment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(doctor, { doctor = it }, label = { Text("Doctor / clinic") }, singleLine = true)
                OutlinedTextField(purpose, { purpose = it }, label = { Text("Purpose") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true)
                DateField("Date", at, { it?.let { d -> at = d } })
                TimeField("Time", at) { at = it }
            }
        },
        confirmButton = { TextButton(enabled = doctor.isNotBlank(), onClick = { onSave(AppointmentEntity(doctor = doctor, purpose = purpose, location = location, dateTime = at)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TipsScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val s = snap ?: return
    var cat by remember { mutableStateOf("All") }
    val eligible = HealthTipsEngine.eligible(s.conditions, s.allergies)
    val daily = HealthTipsEngine.dailyTip(s.conditions, s.allergies)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Health tips", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Palette.primaryGradient) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Today's tip", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f)); Pill(daily.category, Palette.Cyan) }
                    Text(daily.title, style = MaterialTheme.typography.titleLarge)
                    Text(daily.body, style = MaterialTheme.typography.bodyMedium)
                    if (daily.personalized) Text("Shown because of a condition in your profile — confirm with your doctor.", style = MaterialTheme.typography.labelSmall, color = Palette.Amber)
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), accent = Palette.warmGradient) {
                    Text("Weekly wellness challenge", style = MaterialTheme.typography.labelLarge)
                    Text(HealthTipsEngine.weeklyChallenge(), style = MaterialTheme.typography.titleMedium)
                }
            }
            item { Spacer(Modifier.height(12.dp)); ChipSelector(listOf("All") + HealthTipsEngine.categories, cat, { cat = it }) }
            items(eligible.filter { cat == "All" || it.category == cat }) { t ->
                GlassCard(Modifier.fillMaxWidth().padding(top = 10.dp), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Pill(if (t.personalized) "FOR YOU" else "GENERAL", if (t.personalized) Palette.Amber else Palette.Teal)
                    }
                    Text(t.body, style = MaterialTheme.typography.bodyMedium)
                    Text(t.category, style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                }
            }
            item { Disclaimer("General wellness information filtered against the conditions and allergies you've recorded (e.g. hydration tips are hidden if you've recorded kidney or heart failure). It is not individual medical advice.") }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

private data class BodySystem(val key: String, val name: String, val x: Float, val y: Float, val color: Color, val about: String, val keywords: List<String>, val tip: String)

private val systems = listOf(
    BodySystem("brain", "Brain & nervous system", 0.5f, 0.08f, Palette.Purple, "The brain and nerves control movement, sensation, thought and mood.", listOf("migraine", "stroke", "epilep", "anxiety", "depress", "neuro"), "Regular sleep, activity and social connection support brain health."),
    BodySystem("lungs", "Lungs & respiratory system", 0.42f, 0.3f, Palette.Cyan, "Lungs move oxygen into the blood and carbon dioxide out.", listOf("asthma", "copd", "lung", "bronch", "tb"), "Avoid smoke, check air quality, and keep inhalers in date."),
    BodySystem("heart", "Heart & circulation", 0.55f, 0.32f, Palette.Rose, "The heart pumps about 5 litres of blood a minute through arteries and veins.", listOf("hypertension", "blood pressure", "heart", "coronary", "cholesterol", "arrhythm"), "Blood pressure, cholesterol, activity and not smoking matter most."),
    BodySystem("liver", "Liver", 0.42f, 0.43f, Palette.Amber, "The liver processes nutrients and medicines and makes proteins and bile.", listOf("liver", "hepat", "fatty liver", "cirrhosis"), "Limit alcohol and check labels for paracetamol in combination products."),
    BodySystem("stomach", "Stomach & digestion", 0.56f, 0.46f, Palette.Pink, "The digestive system breaks down food and absorbs nutrients.", listOf("gerd", "acid", "ulcer", "ibs", "gastr"), "Smaller meals and fibre support digestion."),
    BodySystem("kidneys", "Kidneys", 0.5f, 0.53f, Palette.Teal, "Kidneys filter waste, balance fluids and salts, and help control BP.", listOf("kidney", "renal", "ckd", "stone"), "Control BP and sugar, and use painkillers carefully."),
    BodySystem("pancreas", "Pancreas & metabolism", 0.5f, 0.48f, Palette.Green, "The pancreas makes insulin to manage blood sugar and enzymes for digestion.", listOf("diabet", "pancrea", "thyroid", "obes"), "Regular meals, activity and medicines as prescribed keep sugar steady."),
    BodySystem("bones", "Bones & joints", 0.38f, 0.78f, Palette.Blue, "Bones support the body; joints allow movement.", listOf("arthritis", "osteo", "gout", "joint", "back"), "Strength exercise, calcium, vitamin D and sunlight support bones.")
)

@Composable
fun BodyExplorerScreen(vm: MediViewModel, back: () -> Unit) {
    val snap by vm.snapshot.collectAsState()
    var selected by remember { mutableStateOf(systems[2]) }
    val reduce = LocalMedi.current.reduceMotion
    val t = rememberInfiniteTransition(label = "body")
    val glow by t.animateFloat(0.4f, if (reduce) 0.4f else 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow")
    val scan by t.animateFloat(0f, if (reduce) 0f else 1f, infiniteRepeatable(tween(3200)), label = "scan")
    val s = snap ?: return
    val conds = s.conditions.map { it.lowercase() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Body explorer", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Palette.primaryGradient) {
                    Box(Modifier.fillMaxWidth().aspectRatio(0.62f)) {
                        Canvas(
                            Modifier.fillMaxSize()
                                .semantics { contentDescription = "Interactive body diagram. Tap an organ to learn about it." }
                                .pointerInput(Unit) {
                                    detectTapGestures { p ->
                                        systems.minByOrNull { sys ->
                                            val dx = sys.x * size.width - p.x; val dy = sys.y * size.height - p.y; dx * dx + dy * dy
                                        }?.let { selected = it }
                                    }
                                }
                        ) {
                            val w = size.width; val h = size.height
                            val body = Brush.verticalGradient(listOf(Palette.Cyan.copy(alpha = 0.35f), Palette.Purple.copy(alpha = 0.25f)))
                            val outline = Palette.Cyan.copy(alpha = 0.7f)
                            // Stylised 3D-style figure built from gradient shapes.
                            drawCircle(body, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.08f))
                            drawCircle(outline, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.08f), style = Stroke(2f))
                            val torso = Path().apply {
                                moveTo(w * 0.34f, h * 0.17f); lineTo(w * 0.66f, h * 0.17f)
                                quadraticBezierTo(w * 0.72f, h * 0.35f, w * 0.64f, h * 0.6f)
                                lineTo(w * 0.36f, h * 0.6f)
                                quadraticBezierTo(w * 0.28f, h * 0.35f, w * 0.34f, h * 0.17f); close()
                            }
                            drawPath(torso, body); drawPath(torso, outline, style = Stroke(2f))
                            fun limb(x1: Float, y1: Float, x2: Float, y2: Float, wd: Float) {
                                drawLine(Palette.Cyan.copy(alpha = 0.3f), Offset(w * x1, h * y1), Offset(w * x2, h * y2), strokeWidth = w * wd, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                            limb(0.33f, 0.2f, 0.2f, 0.5f, 0.07f); limb(0.67f, 0.2f, 0.8f, 0.5f, 0.07f)
                            limb(0.43f, 0.6f, 0.4f, 0.95f, 0.09f); limb(0.57f, 0.6f, 0.6f, 0.95f, 0.09f)
                            // scanning line
                            val sy = h * scan
                            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Palette.Cyan.copy(alpha = 0.25f), Color.Transparent), startY = sy - 30f, endY = sy + 30f), topLeft = Offset(0f, sy - 30f), size = Size(w, 60f))
                            systems.forEach { sys ->
                                val c = Offset(sys.x * w, sys.y * h)
                                val affected = sys.keywords.any { k -> conds.any { it.contains(k) } }
                                val col = if (affected) Palette.Amber else sys.color
                                val r = if (sys == selected) w * 0.05f else w * 0.032f
                                drawCircle(col.copy(alpha = 0.25f * glow), radius = r * 2.2f, center = c)
                                drawCircle(col, radius = r, center = c)
                                if (sys == selected) drawRoundRect(Color.White.copy(alpha = 0.8f), topLeft = Offset(c.x - r * 1.6f, c.y - r * 1.6f), size = Size(r * 3.2f, r * 3.2f), cornerRadius = CornerRadius(r), style = Stroke(2f))
                            }
                        }
                    }
                    Text("Tap a glowing point. Amber points relate to conditions in your profile.", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                }
            }
            item {
                val related = s.records.filter { r -> selected.keywords.any { r.title.lowercase().contains(it) } }
                GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(selected.name, style = MaterialTheme.typography.titleLarge, color = selected.color)
                    Text(selected.about, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Wellness: ${selected.tip}", style = MaterialTheme.typography.bodyMedium)
                    if (related.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("In your records:", style = MaterialTheme.typography.titleSmall)
                        related.forEach { Text("• ${it.title}", style = MaterialTheme.typography.bodyMedium) }
                    }
                    Disclaimer("Educational illustration — not anatomically exact.")
                }
            }
            item { Spacer(Modifier.height(10.dp)); ChipSelector(systems.map { it.key }, selected.key, { k -> selected = systems.first { it.key == k } }) { k -> systems.first { it.key == k }.name.substringBefore(" ") } }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
