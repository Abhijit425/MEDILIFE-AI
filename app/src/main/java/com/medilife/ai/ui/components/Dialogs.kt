package com.medilife.ai.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.DataSource
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.MeasureType
import com.medilife.ai.data.MeasurementEntity
import com.medilife.ai.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
val dateTimeFmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
fun fmtDate(ms: Long): String = dateFmt.format(Date(ms))
fun fmtDateTime(ms: Long): String = dateTimeFmt.format(Date(ms))

@Composable
fun ChipSelector(options: List<String>, selected: String, onSelect: (String) -> Unit, label: (String) -> String = { it }) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: Long?, onChange: (Long?) -> Unit, allowClear: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { open = true }) {
            Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp)); Text("  $label: ${value?.let { fmtDate(it) } ?: "not set"}")
        }
        if (allowClear && value != null) TextButton(onClick = { onChange(null) }) { Text("Clear") }
    }
    if (open) {
        // DatePicker works in UTC millis; convert to/from local start of day.
        val initUtc = value?.let { DoseSchedule.dayOf(it).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        val st = rememberDatePickerState(initialSelectedDateMillis = initUtc)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        val keepTime = value?.let { Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.of(9, 0)
                        onChange(DoseSchedule.epoch(date, keepTime))
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(st) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val st = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(st.hour, st.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(st) }
    )
}

@Composable
fun TimeField(label: String, value: Long, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val t = Instant.ofEpochMilli(value).atZone(java.time.ZoneId.systemDefault())
    OutlinedButton(onClick = { open = true }) { Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)); Text("  $label: ${dateTimeFmt.format(Date(value)).substringAfter(", ")}") }
    if (open) TimePickDialog(t.toLocalTime(), { open = false }) { lt ->
        onChange(DoseSchedule.epoch(t.toLocalDate(), lt)); open = false
    }
}

@Composable
fun AddMeasurementDialog(initialType: String, onDismiss: () -> Unit, onSave: (MeasurementEntity) -> Unit) {
    var type by remember { mutableStateOf(initialType) }
    var v1 by remember { mutableStateOf("") }
    var v2 by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var at by remember { mutableStateOf(System.currentTimeMillis()) }
    val n1 = v1.replace(',', '.').toDoubleOrNull()
    val n2 = v2.replace(',', '.').toDoubleOrNull()
    val valid = n1 != null && (type != MeasureType.BLOOD_PRESSURE || n2 != null) && plausible(type, n1, n2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reading") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChipSelector(MeasureType.all, type, { type = it }) { MeasureType.label(it) }
                if (type == MeasureType.BLOOD_PRESSURE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(v1, { v1 = it }, label = { Text("Systolic") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(v2, { v2 = it }, label = { Text("Diastolic") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                } else {
                    OutlinedTextField(v1, { v1 = it }, label = { Text("${MeasureType.label(type)} (${MeasureType.unit(type)})") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(note, { note = it }, label = { Text(if (type == MeasureType.GLUCOSE) "Note (e.g. fasting / after meal)" else "Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DateField("Date", at, { it?.let { d -> at = d } })
                TimeField("Time", at) { at = it }
                if (n1 != null && !plausible(type, n1, n2)) Text("That value looks outside a possible range — please check it.", color = Palette.Rose, style = MaterialTheme.typography.bodySmall)
                Text("Saved as a MANUAL entry. MediLife never invents readings.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(MeasurementEntity(type = type, value = n1!!, value2 = if (type == MeasureType.BLOOD_PRESSURE) n2 else null, unit = MeasureType.unit(type), timestamp = at, source = DataSource.MANUAL, note = note))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun plausible(type: String, a: Double?, b: Double?): Boolean {
    a ?: return false
    return when (type) {
        MeasureType.HEART_RATE -> a in 20.0..250.0
        MeasureType.BLOOD_PRESSURE -> a in 50.0..260.0 && (b == null || b in 30.0..160.0)
        MeasureType.SPO2 -> a in 50.0..100.0
        MeasureType.WEIGHT -> a in 2.0..350.0
        MeasureType.STEPS -> a in 0.0..100000.0
        MeasureType.SLEEP -> a in 0.0..24.0
        MeasureType.GLUCOSE -> a in 10.0..900.0
        MeasureType.TEMPERATURE -> a in 30.0..45.0
        else -> true
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Confirm", danger: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = if (danger) Palette.Rose else Color.Unspecified, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PassphraseDialog(title: String, confirmTwice: Boolean, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val ok = p1.length >= 6 && (!confirmTwice || p1 == p2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The backup is encrypted with this passphrase. MediLife can't recover it if you forget it.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(p1, { p1 = it }, label = { Text("Passphrase (min 6 characters)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (confirmTwice) OutlinedTextField(p2, { p2 = it }, label = { Text("Repeat passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onOk(p1) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun Avatar(uri: String, name: String, size: Dp = 52.dp) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) {
        value = if (uri.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, Uri.parse(uri))) { d, info, _ ->
                    val s = maxOf(1, maxOf(info.size.width, info.size.height) / 256)
                    d.setTargetSize(info.size.width / s, info.size.height / s)
                }
            }.getOrNull()
        }
    }
    Box(Modifier.size(size).clip(CircleShape).background(Palette.primaryGradient), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), "Profile photo", Modifier.size(size), contentScale = ContentScale.Crop)
        else Text(name.trim().take(1).uppercase().ifBlank { "M" }, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}
