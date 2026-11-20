package com.medilife.ai.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.LabFlag
import com.medilife.ai.engine.LabGlossary
import com.medilife.ai.engine.LabParser
import com.medilife.ai.engine.ParsedLabRow
import com.medilife.ai.ui.MediViewModel
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
import com.medilife.ai.ui.components.fmtDate
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette

fun flagColor(f: String) = when (f) { LabFlag.HIGH -> Palette.Rose; LabFlag.LOW -> Palette.Amber; LabFlag.NORMAL -> Palette.Teal; else -> Palette.Blue }

@Composable
fun ReportsScreen(vm: MediViewModel, back: () -> Unit, go: (String) -> Unit) {
    val reports by vm.reports.collectAsState()
    val ocr by vm.ocr.collectAsState()
    val ctx = LocalContext.current
    var cameraConsent by remember { mutableStateOf(false) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.runOcr(ctx, it, true) } }
    val img = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.runOcr(ctx, it, false) } }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? -> bmp?.let { vm.runOcrBitmap(it) } }

    if (ocr.running || ocr.rawText.isNotBlank() || ocr.error != null) { ReviewScreen(vm, back); return }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar("Health reports", back)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Palette.tealGradient) {
                    Text("Import a lab report", style = MaterialTheme.typography.titleMedium)
                    Text("Text is read on your device with on-device OCR. Nothing is uploaded. You'll review every value before it's saved.", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pdf.launch(arrayOf("application/pdf")) }) { Icon(Icons.Rounded.PictureAsPdf, null); Text(" PDF") }
                        OutlinedButton(onClick = { img.launch(arrayOf("image/*")) }) { Icon(Icons.Rounded.Image, null); Text(" Photo") }
                        OutlinedButton(onClick = { cameraConsent = true }) { Icon(Icons.Rounded.CameraAlt, null); Text(" Camera") }
                    }
                }
            }
            item { SectionHeader("Your reports") }
            if (reports.isEmpty()) item { EmptyState(Icons.Rounded.Science, "No reports yet", "Import a PDF or photo of a lab report to track results over time.") }
            items(reports, key = { it.id }) { r ->
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { go("report/${r.id}") }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Description, Palette.tealGradient, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall)
                            Text("${fmtDate(r.reportDate)} · ${r.labName.ifBlank { r.sourceType }}", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                        }
                        if (r.isDemo) DemoBadge()
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
    if (cameraConsent) AlertDialog(
        onDismissRequest = { cameraConsent = false },
        title = { Text("Use the camera?") },
        text = { Text("MediLife will open your phone's camera app to photograph a report. The photo is processed on this device only and is not stored or uploaded — only the values you confirm are saved.") },
        confirmButton = { TextButton(onClick = { cameraConsent = false; cam.launch(null) }) { Text("Open camera") } },
        dismissButton = { TextButton(onClick = { cameraConsent = false }) { Text("Cancel") } }
    )
}

@Composable
private fun ReviewScreen(vm: MediViewModel, back: () -> Unit) {
    val ocr by vm.ocr.collectAsState()
    val rows = remember { mutableStateListOf<ParsedLabRow>() }
    var title by remember { mutableStateOf("") }
    var lab by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var showRaw by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(ocr.rows) { rows.clear(); rows.addAll(ocr.rows) }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        MediTopBar("Review extracted results", { vm.resetOcr() })
        when {
            ocr.running -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Reading your report on-device…")
            }
            ocr.error != null -> Column(Modifier.padding(18.dp)) {
                Text(ocr.error ?: "", color = Palette.Rose); Button(onClick = { vm.resetOcr() }) { Text("Back") }
            }
            else -> LazyColumn(Modifier.padding(horizontal = 18.dp)) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        OutlinedTextField(title, { title = it }, label = { Text("Report title (e.g. Lipid profile)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(lab, { lab = it }, label = { Text("Lab name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        DateField("Report date", date, { date = it })
                        Text("Please confirm the date printed on the report — results are charted by this date.", style = MaterialTheme.typography.labelSmall, color = Palette.Amber)
                    }
                }
                item {
                    SectionHeader("Detected tests (${rows.size})", if (showRaw) "Hide text" else "Show text") { showRaw = !showRaw }
                    Disclaimer("Check each value against your report. Tap a row to correct it. Rows without a printed reference range won't be compared. If anything is unclear, edit or delete it rather than guessing.")
                }
                if (showRaw) item { GlassCard(Modifier.fillMaxWidth()) { Text(ocr.rawText.take(6000), style = MaterialTheme.typography.bodySmall) } }
                if (rows.isEmpty()) item { Text("No test rows were recognised. You can still save the report text, or try a clearer photo.", style = MaterialTheme.typography.bodyMedium) }
                itemsIndexed(rows) { i, r ->
                    val flag = LabParser.flagFor(r.value, r.refLow, r.refHigh)
                    GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { editing = i }, padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.testName, style = MaterialTheme.typography.titleSmall)
                                Text("${r.valueText} ${r.unit}  ·  range: ${r.refText.ifBlank { "not found" }}", style = MaterialTheme.typography.bodySmall)
                                if (!r.known) Text("Unrecognised test name — please verify", style = MaterialTheme.typography.labelSmall, color = Palette.Amber)
                            }
                            Pill(flag, flagColor(flag))
                            IconButton(onClick = { rows.removeAt(i) }) { Icon(Icons.Rounded.Delete, "Remove row") }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { rows.add(ParsedLabRow("", "", null, "", null, null, "", false)); editing = rows.lastIndex }) { Text("+ Add row") }
                        Button(enabled = date != null, onClick = { vm.saveParsedReport(title, lab, date!!, rows.filter { it.testName.isNotBlank() }) }) { Text("Save report") }
                    }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
    val idx = editing
    if (idx != null && idx in rows.indices) EditRowDialog(rows[idx], { editing = null }) { rows[idx] = it; editing = null }
}

@Composable
private fun EditRowDialog(r: ParsedLabRow, onDismiss: () -> Unit, onSave: (ParsedLabRow) -> Unit) {
    var name by remember { mutableStateOf(r.testName) }
    var value by remember { mutableStateOf(r.valueText) }
    var unit by remember { mutableStateOf(r.unit) }
    var lo by remember { mutableStateOf(r.refLow?.toString() ?: "") }
    var hi by remember { mutableStateOf(r.refHigh?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Test name") }, singleLine = true)
                OutlinedTextField(value, { value = it }, label = { Text("Value") }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit (as printed)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(lo, { lo = it }, label = { Text("Range low") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(hi, { hi = it }, label = { Text("Range high") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Copy the reference range printed on your report. Leave blank if none is printed.", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val l = lo.replace(',', '.').toDoubleOrNull(); val h = hi.replace(',', '.').toDoubleOrNull()
                onSave(ParsedLabRow(name.trim(), value.trim(), value.trim().trimStart('<', '>').replace(',', '.').toDoubleOrNull(), unit.trim(), l, h,
                    when { l != null && h != null -> "$lo - $hi"; h != null -> "< $hi"; l != null -> "> $lo"; else -> "" }, LabGlossary.find(name) != null))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReportDetailScreen(vm: MediViewModel, id: Long, back: () -> Unit) {
    val report by vm.report(id).collectAsState(initial = null)
    val results by vm.resultsFor(id).collectAsState(initial = emptyList())
    val snap by vm.snapshot.collectAsState()
    var expanded by remember { mutableStateOf<Long?>(null) }
    var showText by remember { mutableStateOf(false) }
    val r = report
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        MediTopBar(r?.title ?: "Report", back) { IconButton(onClick = { vm.deleteReport(id); back() }) { Icon(Icons.Rounded.Delete, "Delete report") } }
        if (r == null) return@Column
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = Palette.tealGradient) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleLarge)
                            Text("${fmtDate(r.reportDate)} · ${r.labName.ifBlank { r.sourceType }}", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                        }
                        if (r.isDemo) DemoBadge()
                    }
                    val out = results.count { it.flag == LabFlag.HIGH || it.flag == LabFlag.LOW }
                    Text("${results.size} results · $out outside the printed range", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item { SectionHeader("Results") }
            items(results, key = { it.id }) { x ->
                val history = snap?.results?.filter { it.testName.equals(x.testName, true) && it.value != null }?.sortedBy { it.reportDate } ?: emptyList()
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { expanded = if (expanded == x.id) null else x.id }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(x.testName, style = MaterialTheme.typography.titleSmall)
                            Text("Range on report: ${x.refText.ifBlank { "none printed" }}", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
                        }
                        Text("${x.valueText} ${x.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = flagColor(x.flag))
                        Spacer(Modifier.width(8.dp))
                        Pill(x.flag, flagColor(x.flag))
                    }
                    if (expanded == x.id) {
                        Spacer(Modifier.height(8.dp))
                        Text(LabParser.explain(x), style = MaterialTheme.typography.bodyMedium)
                        if (history.size >= 2) {
                            Spacer(Modifier.height(8.dp))
                            Text("History (${history.size} reports)", style = MaterialTheme.typography.titleSmall)
                            LineChart(history.map { it.value!!.toFloat() }, color = flagColor(x.flag), refLow = x.refLow?.toFloat(), refHigh = x.refHigh?.toFloat(), description = "${x.testName} history")
                            Text(history.joinToString("  →  ") { "${it.valueText} (${fmtDate(it.reportDate)})" }, style = MaterialTheme.typography.labelSmall)
                            Text("Units or labs may differ between reports — compare carefully.", style = MaterialTheme.typography.labelSmall, color = Palette.Amber)
                        }
                    }
                }
            }
            if (r.rawText.isNotBlank()) item {
                TextButton(onClick = { showText = !showText }) { Text(if (showText) "Hide extracted text" else "Show extracted text") }
                if (showText) GlassCard(Modifier.fillMaxWidth()) { Text(r.rawText.take(8000), style = MaterialTheme.typography.bodySmall) }
            }
            item { Disclaimer("MediLife compares values only with the reference range printed on your report and never diagnoses from lab values alone. Age, sex, units, lab methods and your clinical context all matter — discuss results with your doctor.") }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
