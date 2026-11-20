package com.medilife.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sick
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.DoseStatus
import com.medilife.ai.data.HealthSnapshot
import com.medilife.ai.data.MeasureType
import com.medilife.ai.engine.AlertLevel
import com.medilife.ai.engine.HealthTipsEngine
import com.medilife.ai.engine.MediAiEngine
import com.medilife.ai.engine.WellnessScoreCalc
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.AddMeasurementDialog
import com.medilife.ai.ui.components.AnimatedRing
import com.medilife.ai.ui.components.Avatar
import com.medilife.ai.ui.components.DemoBadge
import com.medilife.ai.ui.components.GlassCard
import com.medilife.ai.ui.components.HeartbeatIcon
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.Pill
import com.medilife.ai.ui.components.PulsingDot
import com.medilife.ai.ui.components.SectionHeader
import com.medilife.ai.ui.components.SourceBadge
import com.medilife.ai.ui.components.fmtDateTime
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: MediViewModel, go: (String) -> Unit) {
    val snap by vm.snapshot.collectAsState()
    val demo by vm.demoActive.collectAsState()
    val alerts by vm.alerts.collectAsState()
    val wellness by vm.wellness.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    var addType by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val s = snap ?: return

    LazyColumn(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item { Header(s, now, go) }
        if (demo) item {
            GlassCard(Modifier.fillMaxWidth().padding(top = 12.dp), accent = Brush.linearGradient(listOf(Palette.Amber, Palette.Pink))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DemoBadge(); Spacer(Modifier.width(8.dp))
                    Text("You're exploring demonstration data, not real records.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.removeDemo() }) { Text("Remove") }
                }
            }
        }
        item {
            AnimatedVisibility(visible, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }) { ScoreCard(wellness?.score, wellness?.parts ?: emptyList()) }
        }
        item { SectionHeader("Health overview", "Trends") { go("trends") } }
        item { Overview(s) { addType = it } }
        item { SectionHeader("Today's medicines", "All") { go("medicines") } }
        item { Medication(vm, s, go) }
        item { SectionHeader("Insights") }
        item { Insights(s, alerts.count { it.level == AlertLevel.HIGH }, alerts.size, go) }
        item { SectionHeader("Explore") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile("Body explorer", Icons.Rounded.AccessibilityNew, Palette.primaryGradient, Modifier.weight(1f)) { go("body") }
                QuickTile("Lab reports", Icons.Rounded.Science, Palette.tealGradient, Modifier.weight(1f)) { go("reports") }
                QuickTile("Emergency", Icons.Rounded.Sos, Palette.dangerGradient, Modifier.weight(1f)) { go("emergency") }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
    addType?.let { t -> AddMeasurementDialog(t, { addType = null }) { vm.saveMeasurement(it); addType = null } }
}

@Composable
private fun Header(s: HealthSnapshot, now: Long, go: (String) -> Unit) {
    val hour = java.time.LocalTime.now().hour
    val greet = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val name = s.profile?.name?.substringBefore(" ")?.ifBlank { null }
    val personal = when {
        s.medicines.none { it.active } && s.measurements.isEmpty() -> "Let's set up your health companion."
        DoseSchedule.nextDose(s.medicines, s.doseLogs) != null -> "Your next medicine is coming up — you've got this."
        else -> "Small healthy habits add up. How are you feeling today?"
    }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(SimpleDateFormat("EEEE, d MMMM · h:mm a", Locale.getDefault()).format(Date(now)), style = MaterialTheme.typography.labelMedium, color = LocalMedi.current.subtle)
            Text("$greet${name?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.headlineMedium)
            Text(personal, style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle)
        }
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.layout.Box(Modifier.size(54.dp).padding(1.dp)) {
            TextButton(onClick = { go("profile") }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Avatar(s.profile?.photoUri ?: "", s.profile?.name ?: "", 52.dp)
            }
        }
    }
}

@Composable
private fun ScoreCard(score: Int?, parts: List<Pair<String, Int>>) {
    GlassCard(Modifier.fillMaxWidth().padding(top = 14.dp), accent = Brush.linearGradient(listOf(Palette.Purple, Palette.Cyan))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedRing((score ?: 0) / 100f, size = 118.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HeartbeatIcon(size = 18.dp)
                    Text(score?.toString() ?: "—", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(WellnessScoreCalc.LABEL, style = MaterialTheme.typography.titleMedium)
                if (score == null) Text("Log a few readings or medicine doses to see how your habits are going.", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                parts.forEach { (k, v) -> Text("$k · $v%", style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(4.dp))
                Text("Habits only — not a clinical measurement.", style = MaterialTheme.typography.labelSmall, color = Palette.Amber)
            }
        }
    }
}

private data class Metric(val type: String?, val title: String, val icon: ImageVector, val color: Color)

@Composable
private fun Overview(s: HealthSnapshot, onAdd: (String) -> Unit) {
    val metrics = listOf(
        Metric(MeasureType.HEART_RATE, "Heart rate", Icons.Rounded.Favorite, Palette.Rose),
        Metric(MeasureType.BLOOD_PRESSURE, "Blood pressure", Icons.Rounded.Speed, Palette.Purple),
        Metric(MeasureType.SPO2, "Oxygen", Icons.Rounded.Air, Palette.Cyan),
        Metric(MeasureType.WEIGHT, "Weight", Icons.Rounded.MonitorWeight, Palette.Teal),
        Metric(null, "BMI", Icons.Rounded.WaterDrop, Palette.Blue),
        Metric(MeasureType.STEPS, "Activity", Icons.AutoMirrored.Rounded.DirectionsWalk, Palette.Green),
        Metric(MeasureType.SLEEP, "Sleep", Icons.Rounded.Bedtime, Palette.Pink),
        Metric(MeasureType.GLUCOSE, "Glucose", Icons.Rounded.WaterDrop, Palette.Amber)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { m -> MetricTile(s, m, Modifier.weight(1f)) { m.type?.let(onAdd) ?: onAdd(MeasureType.WEIGHT) } }
            }
        }
    }
}

@Composable
private fun MetricTile(s: HealthSnapshot, m: Metric, modifier: Modifier, onClick: () -> Unit) {
    val latest = m.type?.let { s.latest(it) }
    val bmi = if (m.type == null) {
        val h = s.profile?.heightCm; val w = s.latest(MeasureType.WEIGHT)?.value ?: s.profile?.weightKg
        if (h != null && w != null && h > 50) w / ((h / 100) * (h / 100)) else null
    } else null
    GlassCard(modifier, onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(m.icon, null, tint = m.color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(m.title, style = MaterialTheme.typography.labelMedium, color = LocalMedi.current.subtle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (latest != null && latest.timestamp > System.currentTimeMillis() - 86_400_000) PulsingDot(m.color)
        }
        Spacer(Modifier.height(8.dp))
        when {
            latest != null -> {
                Text(MediAiEngine.fmtMeasure(latest.type, latest.value, latest.value2), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(latest.source); Spacer(Modifier.width(6.dp))
                    Text(fmtDateTime(latest.timestamp), style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle, maxLines = 1)
                }
            }
            bmi != null -> {
                Text("%.1f".format(bmi), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Pill("CALCULATED", Palette.Blue)
            }
            else -> {
                Text("No data", style = MaterialTheme.typography.titleMedium, color = LocalMedi.current.subtle)
                Text(if (m.type == null) "Add height & weight" else "Tap to add", style = MaterialTheme.typography.labelSmall, color = m.color)
            }
        }
    }
}

@Composable
private fun Medication(vm: MediViewModel, s: HealthSnapshot, go: (String) -> Unit) {
    val doses = DoseSchedule.dosesFor(LocalDate.now(), s.medicines, s.doseLogs)
    val adherence = DoseSchedule.weeklyAdherence(s.medicines, s.doseLogs)
    val next = DoseSchedule.nextDose(s.medicines, s.doseLogs)
    GlassCard(Modifier.fillMaxWidth(), accent = Palette.tealGradient) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedRing((adherence ?: 0) / 100f, size = 70.dp, stroke = 8.dp, colors = listOf(Palette.Teal, Palette.Cyan, Palette.Teal)) {
                Text(adherence?.let { "$it%" } ?: "—", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Weekly adherence", style = MaterialTheme.typography.titleSmall)
                Text(next?.let { "Next: ${it.medicine.name} at ${it.time}" + if (DoseSchedule.dayOf(it.at) != LocalDate.now()) " tomorrow" else "" } ?: "No upcoming scheduled doses",
                    style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                Text("${doses.count { it.status == DoseStatus.TAKEN }} taken · ${doses.count { it.status == DoseStatus.SKIPPED }} skipped · ${doses.count { it.status == DoseStatus.PENDING }} pending",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        if (doses.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("No doses scheduled today.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { go("medicine_edit/0") }) { Text("Add a medicine") }
        }
        doses.forEach { d ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Medication, when (d.status) { DoseStatus.TAKEN -> Palette.tealGradient; DoseStatus.SKIPPED -> Palette.warmGradient; else -> Palette.primaryGradient }, 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${d.medicine.name} ${d.medicine.strength}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${d.time} · ${d.medicine.dosage.ifBlank { "as prescribed" }}", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                        if (d.medicine.isDemo) { Spacer(Modifier.width(6.dp)); DemoBadge() }
                    }
                }
                when (d.status) {
                    DoseStatus.PENDING -> {
                        FilledTonalIconButton(onClick = { vm.setDose(d.medicine.id, d.at, DoseStatus.TAKEN) }) { Icon(Icons.Rounded.Check, "Mark taken") }
                        FilledTonalIconButton(onClick = { vm.setDose(d.medicine.id, d.at, DoseStatus.SKIPPED) }) { Icon(Icons.Rounded.Close, "Mark skipped") }
                    }
                    else -> TextButton(onClick = { vm.setDose(d.medicine.id, d.at, null) }) {
                        Text(if (d.status == DoseStatus.TAKEN) "Taken ✓" else "Skipped", color = if (d.status == DoseStatus.TAKEN) Palette.Teal else Palette.Amber)
                    }
                }
            }
        }
    }
}

@Composable
private fun Insights(s: HealthSnapshot, highAlerts: Int, totalAlerts: Int, go: (String) -> Unit) {
    val tip = HealthTipsEngine.dailyTip(s.conditions, s.allergies)
    val recentSym = s.symptoms.filter { it.timestamp > System.currentTimeMillis() - 14L * 86_400_000 }.groupBy { it.name }.maxByOrNull { it.value.size }
    val nextAppt = s.appointments.firstOrNull { it.dateTime > System.currentTimeMillis() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightRow(Icons.Rounded.Lightbulb, Palette.primaryGradient, "Tip · ${tip.category}", tip.title + " — " + tip.body, if (tip.personalized) "Personalised" else "General wellness") { go("tips") }
        InsightRow(Icons.Rounded.Shield, if (highAlerts > 0) Palette.dangerGradient else Palette.tealGradient, "Medication safety",
            if (totalAlerts == 0) "No alerts from the offline safety checks." else "$totalAlerts alert(s), $highAlerts high priority. Tap to see why.", "Not comprehensive") { go("interactions") }
        InsightRow(Icons.Rounded.Sick, Palette.warmGradient, "Symptom trend",
            recentSym?.let { "${it.key} logged ${it.value.size}× in 14 days (avg severity ${"%.1f".format(it.value.map { x -> x.severity }.average())}/10)." } ?: "No symptoms logged in the last 14 days.", null) { go("symptoms") }
        InsightRow(Icons.Rounded.CalendarMonth, Palette.tealGradient, "Upcoming check-up",
            nextAppt?.let { "${it.purpose.ifBlank { "Appointment" }} with ${it.doctor} · ${fmtDateTime(it.dateTime)}" } ?: "No appointments scheduled.", null) { go("appointments") }
    }
}

@Composable
private fun InsightRow(icon: ImageVector, brush: Brush, title: String, body: String, tag: String?, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, brush, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    tag?.let { Pill(it, Palette.Cyan) }
                }
                Text(body, style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, icon: ImageVector, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, onClick = onClick, padding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBubble(icon, brush, 42.dp)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
